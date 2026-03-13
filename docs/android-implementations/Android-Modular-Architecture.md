# 5-Tier Modular Architecture Skeleton

A scalable modular architecture designed for large applications using:

- Clean Architecture
- Feature-based modularization
- Shared cross-feature modules
- Core infrastructure modules
- Strict data → domain → presentation layering

Navigation lives **inside the presentation layer**.  
Contracts/interfaces live **inside the domain layer**.

---

# Architecture Overview


app
│
├── feature modules
│ ├── feature:home
│ ├── feature:transfer
│ ├── feature:cards
│ ├── feature:history
│ ├── feature:bills
│ └── feature:profile
│
├── shared modules
│ ├── shared:auth
│ └── shared:payment
│
├── common modules
│ ├── common:domain
│ ├── common:data
│ └── common:presentation
│
└── core modules
├── core:network
├── core:database
├── core:ui
├── core:navigation
└── core:contracts


---

# App Module

Entry point of the entire application.


:app
│
├── MyApplication.kt
├── MainActivity.kt
├── AppDatabase.kt
└── NavigationAppNavGraph.kt


Responsibilities:

- Application initialization
- Dependency injection setup
- Root navigation graph
- Database initialization
- Feature module integration

---

# Feature Modules

Each feature module is **self-contained** and follows a strict layered architecture.


:feature:<feature-name>


Example modules:


:feature:home
:feature:transfer
:feature:cards
:feature:history
:feature:bills
:feature:profile


Each feature contains:


feature
│
├── data
├── domain
└── presentation


---

# Feature Internal Structure

Example: `feature:transfer`


feature:transfer
│
├── data
│ │
│ ├── remote
│ │ ├── TransferApi.kt
│ │ └── TransferDto.kt
│ │
│ ├── local
│ │ ├── TransferDao.kt
│ │ └── TransferEntity.kt
│ │
│ ├── mapper
│ │ └── TransferMapper.kt
│ │
│ ├── repository
│ │ └── TransferRepositoryImpl.kt
│ │
│ └── di
│ └── TransferDataModule.kt
│
├── domain
│ │
│ ├── model
│ │ ├── TransferRequest.kt
│ │ └── Beneficiary.kt
│ │
│ ├── repository
│ │ └── TransferRepository.kt
│ │
│ └── usecase
│ └── InitiateTransferUseCase.kt
│
└── presentation
│
├── amount
│ ├── AmountScreen.kt
│ └── AmountViewModel.kt
│
├── confirm
│ ├── ConfirmScreen.kt
│ └── ConfirmViewModel.kt
│
├── success
│ ├── SuccessScreen.kt
│ └── SuccessViewModel.kt
│
└── navigation
├── TransferRoutes.kt
└── TransferNavGraph.kt


---

# Shared Feature Modules

Shared modules provide **cross-feature functionality**.

Example:


:shared:auth
:shared:payment


---

## Shared Auth (OTP Verification)


shared:auth
│
├── data
│ ├── OtpApi.kt
│ ├── OtpRequestDto.kt
│ ├── VerifyOtpDto.kt
│ └── OtpRepositoryImpl.kt
│
├── domain
│ ├── OtpConfig.kt
│ ├── OtpVerificationResult.kt
│ └── usecase
│ ├── RequestOtpUseCase.kt
│ └── VerifyOtpUseCase.kt
│
└── presentation
├── OtpScreen.kt
├── OtpViewModel.kt
└── navigation
└── AuthNavGraph.kt


---

## Shared Payment (Processing)


shared:payment
│
├── data
│ ├── PaymentGatewayApi.kt
│ └── PaymentDto.kt
│
├── domain
│ ├── PaymentRequest.kt
│ └── ProcessPaymentUseCase.kt
│
└── presentation
├── PaymentScreen.kt
└── CardInputField.kt


---

# Common Modules

Shared utilities and abstractions used across the application.

---

## Common Domain


common:domain
│
├── models
│ ├── Account.kt
│ ├── Money.kt
│ ├── User.kt
│ └── Transaction.kt
│
├── interfaces
│ ├── UserRepository.kt
│ └── SessionRepository.kt
│
├── resources
│ ├── Resource.kt
│ └── UiText.kt
│
└── utils
└── DispatcherProvider.kt


---

## Common Data


common:data
│
├── remote
│ ├── UserApi.kt
│ └── AccountApi.kt
│
├── local
│ ├── UserDao.kt
│ └── AccountDao.kt
│
├── entities
│ ├── UserEntity.kt
│ └── AccountEntity.kt
│
├── mapper
│ └── EntityMappers.kt
│
└── repository
└── UserRepositoryImpl.kt


---

## Common Presentation

Reusable UI components and base classes.


common:presentation
│
├── base
│ ├── BaseViewModel.kt
│ ├── UiState.kt
│ └── UiEvent.kt
│
├── components
│ ├── AccountCard.kt
│ ├── MoneyText.kt
│ ├── TransactionRow.kt
│ └── BalanceDisplay.kt
│
├── sheets
│ └── AccountPickerSheet.kt
│
└── utils
└── UiEffects.kt


---

# Core Modules

Core modules contain **low-level infrastructure** used across the entire application.

---

## Core Network


core:network
│
├── NetworkClient.kt
├── AuthInterceptor.kt
├── TokenProvider.kt
├── ApiException.kt
├── ServerErrorDto.kt
└── NetworkModule.kt


---

## Core Database


core:database
│
├── BaseDao.kt
├── TypeConverters.kt
├── TransactionRunner.kt
└── DatabaseModule.kt


---

## Core UI

Reusable UI theme and components.


core:ui
│
├── theme
│ ├── AppTheme.kt
│ ├── AppColors.kt
│ └── AppTypography.kt
│
├── components
│ ├── AppCard.kt
│ ├── AppBadge.kt
│ └── LoadingScreen.kt
│
└── effects
└── ShimmerEffect.kt


---

## Core Navigation

Global navigation contracts.


core:navigation
│
├── EntryRoutes.kt
├── HomeRoute.kt
├── TransferRoute.kt
├── CardsRoute.kt
└── ProfileRoute.kt


---

## Core Contracts

Shared domain contracts used across features.


core:contracts
│
├── TransactionContract.kt
├── TransactionSummary.kt
├── CardContract.kt
└── CardSummary.kt


---

# Model Flow

Application data transformation flow.


DTO → Entity → Domain Model → UI Model


Example:


Server DTO
↓
Database Entity
↓
Domain Model
↓
UI Model


---

# Error Handling Flow

Standardized error propagation pipeline.


Network
↓
Mapper
↓
Data Layer
↓
Domain Layer
↓
ViewModel
↓
UI


Example pipeline:


IOException / HttpException
↓
ErrorMapper
↓
safeApiCall()
↓
Resource.Error
↓
UseCase validation
↓
ViewModel state
↓
UI error display


---

# Architecture Principles

### 1. Feature Independence
Each feature module must be independent and self-contained.

### 2. Layer Isolation


presentation → domain → data


Rules:

- Presentation cannot access data directly
- Domain defines repository interfaces
- Data implements repository interfaces

---

### 3. Dependency Direction


UI → Domain → Data → Core


Never reverse dependencies.

---

### 4. Shared Logic

Reusable logic belongs in:


shared/*
common/*
core/*


---

# Benefits

- Highly scalable architecture
- Independent feature development
- Faster build times via modularization
- Clear separation of concerns
- Reusable shared modules
- Clean testable domain layer

---

# Recommended Usage

Ideal for:

- Fintech apps
- Super apps
- Fleet management systems
- E-commerce platforms
- Large enterprise mobile applications