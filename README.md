# JBDL8 E-Wallet — Microservices Backend

A production-style digital wallet backend built with Spring Boot microservices. A user registers with OTP verification, gets a wallet automatically, and can transfer money to other users — all while every service communicates asynchronously through Apache Kafka. Redis handles ephemeral state. MySQL persists everything that matters.

---

## Table of Contents

1. [What this project demonstrates](#what-this-project-demonstrates)
2. [Architecture overview](#architecture-overview)
3. [The five services](#the-five-services)
4. [The three core flows](#the-three-core-flows)
5. [The three hard problems and how they're solved](#the-three-hard-problems-and-how-theyre-solved)
6. [Tech stack](#tech-stack)
7. [Service-by-service breakdown](#service-by-service-breakdown)

---

## What this project demonstrates

Three things working end to end:

1. **Two-phase user onboarding with OTP gate.** A user's data never touches the database until they prove they own the email. The unverified object lives in Redis with a TTL; the OTP lives in a separate Redis key with its own TTL. Only after the codes match does a Kafka event fire and the user get persisted.

2. **Fully decoupled wallet creation.** No service calls WalletService directly to create a wallet. UserService publishes a `USER_CREATION_TOPIC` event after successful registration; WalletService and NotificationService both consume it independently. If either goes down, they catch up when they come back up — the producer never knows or cares.

3. **Asynchronous money transfer with a status feedback loop.** TransactionService writes the transaction to its own database, publishes it to `TXN_TOPIC`, and returns a transaction ID immediately. WalletService consumes it, does the actual debit/credit, then publishes the outcome to `TXN_UPDATE_TOPIC`. TransactionService consumes that update and marks the transaction SUCCESS or FAILED. No synchronous blocking anywhere in the transfer path.

---

## Architecture overview

```
                        ┌──────────────┐
                        │  UserService │  :8091
                        │  (MySQL DB)  │
                        └──────┬───────┘
                               │ Feign (sync, auth delegation only)
              ┌────────────────┼──────────────────┐
              │                │                  │
              ▼                ▼                  ▼
   ┌──────────────────┐  USER_CREATION_TOPIC  (Kafka)
   │ NotificationSvc  │◀──────────────────────────┤
   │    :8092         │                            │
   │  (SMTP + Redis)  │                            ▼
   └──────────────────┘               ┌─────────────────────┐
                                      │    WalletService     │
   ┌──────────────────┐               │       :8094          │
   │ TransactionSvc   │──TXN_TOPIC──▶ │    (MySQL DB)        │
   │    :8095         │               └──────────┬───────────┘
   │  (MySQL DB)      │◀──TXN_UPDATE_TOPIC───────┘
   └──────────────────┘

   CommonService — shared library: constants, enums, shared models
   Redis         — OTP store (NotificationService) + user object cache (UserService)
   Kafka         — all cross-service events
   OpenFeign     — UserService → NotificationService (OTP trigger)
                 — WalletService → UserService (auth validation)
                 — TransactionService → UserService (auth validation)
```

Every service protects its endpoints with Spring Security HTTP Basic. WalletService and TransactionService do not store user credentials — on each request their `CustomUserDetailsService` calls UserService via Feign to fetch and validate credentials, then builds a Spring Security `UserDetails` object in memory for that request only.

---

## The five services

| Service | Port | Database | Responsibility |
|---|---|---|---|
| **UserService** | 8091 | MySQL (`jbdl8_userdb`) | Registration, OTP flow, credential validation endpoint |
| **NotificationService** | 8092 | — | OTP email dispatch, welcome email after registration |
| **WalletService** | 8094 | MySQL (`jbdl8_walletdb`) | Wallet creation, balance reads, balance updates |
| **TransactionService** | 8095 | MySQL (`jbdl8_txndb`) | Transaction initiation, history, status tracking |
| **CommonService** | library | — | Shared constants, `UserIdentifier` enum |

---

## The three core flows

### Flow 1 — User Registration with OTP verification

```
Client
  │
  ├─POST /user-service/onboard/user ──▶ UserService
  │                                          │
  │                                          ├─▶ Feign POST → NotificationService
  │                                          │   /notification/generate/otp/{email}
  │                                          │        │
  │                                          │        ├── Generate 6-digit OTP
  │                                          │        ├── Store in Redis: key="{email}OTP"
  │                                          │        └── Send OTP email via JavaMailSender
  │                                          │
  │                                          └── Store User object in Redis: key="{mobile}USER"
  │◀── 200: "OTP Sent" ───────────────────────
  │
  ├─POST /user-service/validate/otp ──▶ UserService
  │                                          │
  │                                          ├── Fetch OTP from Redis (key: "{email}OTP")
  │                                          ├── Compare with submitted OTP
  │                                          ├── Fetch User object from Redis (key: "{mobile}USER")
  │                                          ├── Save User to MySQL
  │                                          └── Publish to Kafka: USER_CREATION_TOPIC
  │◀── 200: savedUser ────────────────────────
  │
  │   [Kafka consumers react asynchronously]
  │
  │   WalletService (WALLET_GROUP)
  │        └── Create Wallet row in MySQL with initial balance
  │
  │   NotificationService (EMAIL_GROUP)
  │        └── Send welcome HTML email via JavaMailSender
```

### Flow 2 — Money Transfer

```
Client (authenticated as sender)
  │
  ├─POST /transaction-service/create/txn ──▶ TransactionService
  │   { receiver: "9999999999", amount: 200, purpose: "dinner" }
  │                                               │
  │                                               ├── Read sender mobile from SecurityContext
  │                                               ├── Save Transaction (status: INITIATE) to MySQL
  │                                               ├── Generate UUID txnId
  │                                               └── Publish to Kafka: TXN_TOPIC
  │◀── 200: txnId ─────────────────────────────────
  │
  │   WalletService consumes TXN_TOPIC:
  │        ├── Fetch sender and receiver wallets
  │        ├── Validate receiver exists and is ACTIVE
  │        ├── Validate sufficient balance
  │        ├── @Transactional: debit sender, credit receiver
  │        └── Publish result to Kafka: TXN_UPDATE_TOPIC
  │            { txnId, status: SUCCESS/FAILED, message }
  │
  │   TransactionService consumes TXN_UPDATE_TOPIC:
  │        └── UPDATE transaction SET status=SUCCESS/FAILED WHERE txnId=...
```

### Flow 3 — Balance Retrieval

```
Client (authenticated)
  │
  ├─GET /wallet-service/get/balance ──▶ WalletService
  │                                          │
  │                                          ├── Spring Security intercepts request
  │                                          ├── CustomUserDetailsService.loadUserByUsername()
  │                                          │   └── Feign GET → UserService /validate/user/{email}
  │                                          │        └── Returns { email, mobileNo, hashedPassword }
  │                                          ├── Spring Security validates password hash
  │                                          ├── Read mobile from SecurityContext
  │                                          └── SELECT balance FROM wallet WHERE mobileNo=mobile
  │◀── 200: balance (Double) ─────────────────
```

---

## The three hard problems and how they're solved

### Problem 1: Don't persist unverified users

If you write a user to the database at registration time and OTP verification happens later, you end up with garbage rows from people who mistyped emails, bots, or anyone who abandoned the flow. Cleaning those up is its own operational problem.

**Solution: Redis as a temporary holding area with TTL.**

When a user submits the registration form, the backend builds the full `User` object — BCrypt-hashes the password, sets the role, sets status to ACTIVE — but stores it *only in Redis*, keyed by `{mobileNo}USER`. The OTP is stored in a separate Redis key `{email}OTP`. Both expire automatically via TTL. No cleanup job needed.

Only when the OTP matches does the server pull the `User` object out of Redis and call `userRepository.save()`, then fire the Kafka event. If OTP verification never happens, both keys vanish when their TTLs expire.

### Problem 2: Wallet creation shouldn't be UserService's job

The instinct when a user registers is to have UserService directly call WalletService to create a wallet. That creates tight coupling — if WalletService is down during registration, the whole registration fails. UserService now has to know about WalletService. Any future service that also needs to react to a new user (loyalty points, KYC service, etc.) requires another change to UserService.

**Solution: Kafka event-driven decoupling.**

UserService publishes a `USER_CREATION_TOPIC` event and forgets about it. WalletService subscribes with consumer group `WALLET_GROUP`. NotificationService subscribes with consumer group `EMAIL_GROUP`. Both react independently. Adding a new downstream reaction means adding a new consumer — UserService never changes. Kafka guarantees the event is retained so if a consumer is temporarily down, it processes the event when it recovers.

### Problem 3: Cross-service authentication without a shared user store

WalletService and TransactionService need to authenticate incoming requests, but they don't have a local copy of user credentials — and they shouldn't, because that creates a sync problem whenever a user changes their password.

**Solution: Delegated authentication via OpenFeign.**

WalletService and TransactionService each implement a `CustomUserDetailsService` that, instead of querying a local database, makes a Feign call to UserService's `/validate/user/{email}` endpoint. UserService returns the email, mobile number, and BCrypt-hashed password for that user. Spring Security then handles password verification locally using the hash it just fetched. The authenticated user's mobile number sits in `SecurityContextHolder` for any subsequent business logic.

The trade-off is honest: every authenticated request to WalletService or TransactionService makes an internal HTTP call to UserService. The natural evolution is JWT-based stateless auth — the token would carry the mobile number as a claim, eliminating the per-request Feign call and removing UserService as a runtime dependency for authentication.

---

## Tech stack

| Technology | Role |
|---|---|
| **Spring Boot 3.x** | Service framework for all four runnable services |
| **Apache Kafka** | Async event bus between services |
| **Redis (Redis Cloud)** | OTP storage and unverified user object cache |
| **MySQL** | Persistent storage for users, wallets, and transactions |
| **Spring Security** | HTTP Basic auth on WalletService and TransactionService |
| **OpenFeign** | Sync HTTP calls between services (OTP trigger, auth delegation) |
| **Spring Data JPA / Hibernate** | ORM for all MySQL interactions |
| **JavaMailSender** | OTP emails and welcome emails via Gmail SMTP |
| **Lombok** | Boilerplate reduction across all services |
| **CommonService** | Internal Maven library for shared constants and enums |

---

## Service-by-service breakdown

### UserService (:8091)

The entry point for all user-facing operations. Owns the `User` entity (`id`, `name`, `email`, `mobileNo`, `password`, `dob`, `role`, `userIdentifier`, `userIdentifierValue`, `userStatus`) in MySQL. Exposes three key endpoints:

- `POST /onboard/user` — validates the request, BCrypt-hashes the password, stores the `User` object in Redis, triggers OTP via Feign to NotificationService. **Publicly accessible.**
- `POST /validate/otp` — compares submitted OTP against Redis, saves the user to MySQL on match, publishes `USER_CREATION_TOPIC`. **Publicly accessible.**
- `GET /validate/user/{userId}` — returns email, mobile, and hashed password as JSON. Called by WalletService and TransactionService for auth delegation. **Publicly accessible** (consumed only by internal services).

Two Redis templates are configured: one typed as `RedisTemplate<String, User>` for the user object cache and one as `RedisTemplate<String, String>` for OTP values. Both are accessed through a `RedisUtil` wrapper component.

### NotificationService (:8092)

Stateless except for Redis. Handles two responsibilities:

- **OTP dispatch** — exposes `POST /notification/generate/otp/{email}`, called synchronously by UserService during registration. Generates a 6-digit random OTP, stores it in Redis as `{email}OTP`, and sends a plain-text email via JavaMailSender.
- **Welcome email** — consumes `USER_CREATION_TOPIC` (group: `EMAIL_GROUP`) and sends an HTML-formatted welcome email with the user's name and KYC document details.

The `Worker` interface with `OTPWorker` as its implementation provides a clean extension point — swapping in an SMS worker or push notification worker requires only a new implementation and no changes to the controller or consumer.

### WalletService (:8094)

Owns the `Wallet` entity (`userId`, `mobileNo`, `walletStatus`, `userIdentifier`, `userIdentifierValue`, `balance`) in MySQL.

Consumes two Kafka topics:
- `USER_CREATION_TOPIC` (group: `WALLET_GROUP`) — creates a new wallet with a configurable initial balance (injected via `wallet.initial.amount`, defaulting to ₹100).
- `TXN_TOPIC` (group: `txn-create-group`) — processes money transfers. Fetches sender and receiver wallets, validates receiver status and balance, runs the debit/credit in a `@Transactional` method using two custom `@Modifying @Query` JPQL statements, then publishes the outcome to `TXN_UPDATE_TOPIC`.

Balance retrieval is the one synchronous path: `GET /wallet-service/get/balance` reads the mobile from `SecurityContextHolder` and queries the wallet table directly.

### TransactionService (:8095)

Owns the `Transaction` entity (`txnId` as UUID with unique constraint, `senderId`, `receiverId`, `transferAmount`, `txnStatus`, `txnMessage`, `purpose`) in MySQL.

- `POST /transaction-service/create/txn` — reads the sender's mobile from the security context, builds a `Transaction` with status `INITIATE`, saves it, publishes to `TXN_TOPIC`, and returns the `txnId` immediately. The async nature means the client gets a response before any money has moved.
- `GET /transaction-service/get/transaction/history` — returns the authenticated user's full transaction history, labeling each entry `USER_DEBIT` (when user is sender) or `USER_CREDIT` (when user is receiver).

Consumes `TXN_UPDATE_TOPIC` to update transaction status from `PENDING` to `SUCCESS` or `FAILED` once WalletService completes the balance operation.

### CommonService (shared library)

A plain Maven JAR (`org.gfg:CommonService:1.0-SNAPSHOT`) declared as a dependency by all other services. Contains `CommonConstants` (Kafka topic names, JSON field key strings, bootstrap server address) and the `UserIdentifier` enum (`AADHAAR_CARD`, `PAN_CARD`, `DRIVING_LICENSE`, `PASSPORT`). Centralizing these prevents divergence — a Kafka topic name change is a one-line edit in one place, not a change across four codebases.
