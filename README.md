# TechnoNext Test - Android Application

A comprehensive Android application built with modern Android development practices, featuring clean architecture, dependency injection, and robust testing. This app demonstrates user authentication, post management, favorites functionality, and offline-first data handling.

## Table of Contents

- [Features](#features)
- [Setup & Build Instructions](#setup--build-instructions)
- [App Architecture](#app-architecture)
- [Libraries & Technologies Used](#libraries--technologies-used)
- [Project Structure](#project-structure)
- [Key Components](#key-components)
- [Testing Strategy](#testing-strategy)
- [Assumptions & Limitations](#assumptions--limitations)
- [Key Insights](#key-insights)

## Features

### Core Functionality
- **User Authentication**: Complete login and registration system with validation
- **Post Management**: View, search, and manage posts with pagination
- **Favorites System**: Mark/unmark posts as favorites with persistent storage
- **Offline Support**: Local database caching for offline functionality
- **Network Monitoring**: Real-time network status indicator
- **Search**: Advanced post search with filtering capabilities
- **User Profile**: Profile management and user preferences

### Technical Features
- **Clean Architecture**: Separation of concerns with data, domain, and presentation layers
- **MVVM Pattern**: ViewModel-based UI state management
- **Dependency Injection**: Hilt for dependency management
- **Reactive Programming**: Coroutines and Flow for asynchronous operations
- **Local Storage**: Room database with DataStore preferences
- **Network Layer**: Retrofit with OkHttp for API communication
- **UI/UX**: Modern Jetpack Compose UI with Material Design 3

## Setup & Build Instructions

### Prerequisites
- **Android Studio**: Arctic Fox or later (recommended: latest stable version)
- **JDK**: Java 11 or higher
- **Android SDK**: API level 24 (Android 7.0) minimum, API 36 target
- **Git**: For version control

### Clone and Setup
```bash
# Clone the repository
git clone <repository-url>
cd TechnoNextTest

# Open in Android Studio
# File -> Open -> Select the project directory
```

### Build Configuration
The project uses Kotlin DSL for Gradle configuration:

#### Minimum Requirements
- **Min SDK**: 24 (Android 7.0 - Nougat)
- **Target SDK**: 36 (Android 14)
- **Compile SDK**: 36
- **Java Version**: 11
- **Kotlin Version**: 1.9.24

#### Build Commands
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Install debug APK
./gradlew installDebug
```

### IDE Configuration
1. **Import Project**: Open Android Studio -> File -> Open -> Select project root
2. **Sync Gradle**: Click "Sync Now" when prompted
3. **SDK Setup**: Ensure Android SDK 36 is installed via SDK Manager
4. **Build Variants**: Select desired build variant (debug/release)

### Running the App
1. **Emulator**: Create AVD with API 24+ via AVD Manager
2. **Physical Device**: Enable Developer Options and USB Debugging
3. **Run**: Click Run button or use `Ctrl+R` (Mac: `Cmd+R`)

### Environment Setup
The app uses JSONPlaceholder API (https://jsonplaceholder.typicode.com/) as the backend service. No additional backend setup is required.

## App Architecture

### Clean Architecture Implementation

The application follows Clean Architecture principles with clear separation of concerns across three main layers:

#### 1. Data Layer (`/data`)
- **Repositories**: Implementation of domain repository interfaces
- **Local Storage**: Room database with DAOs and entities
- **Remote API**: Retrofit services and DTOs
- **Paging**: PagingSource implementations for efficient data loading
- **Preferences**: DataStore for user preferences and settings

#### 2. Domain Layer (`/domain`)
- **Models**: Core business models (Post, User)
- **Repositories**: Abstract repository interfaces
- **Use Cases**: Business logic encapsulation (13 use cases total)
  - Authentication: Login, Register, Logout, GetCurrentUser
  - Posts: GetPosts, GetPaginatedPosts, GetFavorites, ToggleFavorite, Search, etc.

#### 3. Presentation Layer (`/presentation`)
- **ViewModels**: UI state management with MVVM pattern (5 ViewModels)
- **UI Screens**: Jetpack Compose screens for different features
- **Navigation**: Navigation component with Compose integration
- **Theme**: Material Design 3 theming system

### Key Architectural Patterns

#### MVVM (Model-View-ViewModel)
- **Model**: Domain models and repository interfaces
- **View**: Jetpack Compose UI components
- **ViewModel**: State management and business logic coordination

#### Repository Pattern
- Abstract data access through repository interfaces
- Multiple data sources (local database, remote API)
- Single source of truth implementation

#### Use Case Pattern
- Encapsulates business logic
- Reusable across different ViewModels
- Clear separation of concerns

#### Dependency Injection
- Hilt for compile-time dependency injection
- Modular DI configuration (NetworkModule, DatabaseModule, etc.)
- Singleton and scoped component management

## Libraries & Technologies Used

### Core Android
- **Kotlin** (1.9.24): Primary programming language
- **Jetpack Compose** (2024.06.00): Modern declarative UI toolkit
- **Android Core KTX** (1.13.1): Kotlin extensions for Android APIs
- **Activity Compose** (1.10.1): Compose integration for activities

### Architecture & Navigation
- **Navigation Compose** (2.7.7): Type-safe navigation for Compose
- **Lifecycle ViewModel Compose** (2.8.6): ViewModel integration with Compose
- **Lifecycle Runtime Compose** (2.8.6): Lifecycle-aware composables

### Dependency Injection
- **Hilt** (2.48): Compile-time dependency injection
- **Hilt Navigation Compose** (1.2.0): Navigation integration for Hilt

### Networking
- **Retrofit** (2.9.0): Type-safe HTTP client
- **OkHttp Logging Interceptor** (4.12.0): Network request/response logging
- **Gson Converter** (2.9.0): JSON serialization/deserialization

### Database & Storage
- **Room** (2.6.1): SQLite abstraction layer
  - Room Runtime: Core database functionality
  - Room KTX: Kotlin extensions and coroutines support
  - Room Paging: Paging 3 integration
- **DataStore Preferences** (1.0.0): Modern SharedPreferences replacement
- **Security Crypto** (1.1.0-alpha06): Encrypted shared preferences

### Asynchronous Programming
- **Kotlin Coroutines** (1.7.3): Asynchronous programming
- **Paging 3** (3.2.1): Efficient large dataset handling
  - Paging Runtime: Core paging functionality
  - Paging Compose: Compose integration

### UI & Material Design
- **Material 3**: Latest Material Design components
- **Material Icons Extended**: Extended icon set
- **Compose UI Tooling**: Development and debugging tools

### Testing Framework
- **JUnit** (4.13.2): Unit testing framework
- **Mockito** (5.8.0): Mocking framework for tests
- **Mockito Kotlin** (5.2.1): Kotlin-specific Mockito extensions
- **Coroutines Test** (1.7.3): Testing utilities for coroutines
- **Espresso** (3.7.0): UI testing framework

### Build & Development Tools
- **KSP** (1.9.24-1.0.20): Kotlin Symbol Processing for annotation processing
- **Android Gradle Plugin** (8.9.1): Build system
- **JavaPoet** (1.13.0): Java source code generation

## Project Structure

```
app/src/main/java/dev/sadakat/technonexttest/
├── data/                           # Data Layer
│   ├── local/
│   │   ├── database/              # Room Database
│   │   │   ├── dao/               # Data Access Objects
│   │   │   ├── entities/          # Database Entities
│   │   │   └── AppDatabase.kt     # Database Configuration
│   │   └── preferences/           # DataStore Preferences
│   ├── paging/                    # Paging Sources
│   ├── remote/                    # Network Layer
│   │   ├── api/                   # API Services
│   │   └── dto/                   # Data Transfer Objects
│   └── repository/                # Repository Implementations
├── di/                            # Dependency Injection
│   ├── AppModule.kt              # App-level dependencies
│   ├── DatabaseModule.kt         # Database dependencies
│   ├── NetworkModule.kt          # Network dependencies
│   └── RepositoryModule.kt       # Repository dependencies
├── domain/                        # Domain Layer
│   ├── model/                     # Domain Models
│   ├── repository/                # Repository Interfaces
│   └── usecase/                   # Business Logic Use Cases
│       ├── auth/                  # Authentication Use Cases
│       └── posts/                 # Posts Use Cases
├── presentation/                  # Presentation Layer
│   ├── navigation/                # Navigation Configuration
│   ├── ui/                        # UI Screens & Components
│   │   ├── auth/                  # Authentication Screens
│   │   ├── posts/                 # Posts Screens
│   │   ├── search/                # Search Functionality
│   │   ├── favorites/             # Favorites Management
│   │   ├── profile/               # User Profile
│   │   ├── components/            # Reusable UI Components
│   │   └── theme/                 # App Theming
│   └── viewmodel/                 # ViewModels
├── util/                          # Utilities & Helpers
│   ├── AppConfig.kt              # App Configuration
│   ├── NetworkResult.kt          # Network Response Wrapper
│   ├── NetworkMonitor.kt         # Network Status Monitoring
│   ├── PasswordUtils.kt          # Password Utilities
│   └── Mappers.kt                # Data Mapping Extensions
├── MainActivity.kt               # Main Activity
└── TechnoNextApplication.kt      # Application Class
```

## Key Components

### Authentication System
- **Secure Registration**: Password validation, email validation, duplicate user checks
- **Login Management**: Secure authentication with encrypted preferences
- **Session Management**: Persistent login state with automatic logout
- **Password Security**: Hashing and validation utilities

### Posts Management
- **Pagination**: Efficient loading with Paging 3 library
- **Offline-First**: Local caching with Room database
- **Search & Filter**: Real-time search with query optimization
- **Favorites**: Persistent favorite marking with user association

### Data Flow Architecture
- **Single Source of Truth**: Repository pattern ensuring data consistency
- **Reactive Updates**: Flow-based reactive programming
- **Error Handling**: Comprehensive error states and user feedback
- **Loading States**: Progress indicators and skeleton loading

### Network & Caching
- **API Integration**: JSONPlaceholder REST API
- **Intelligent Caching**: Database-first approach with network fallback
- **Network Monitoring**: Real-time connectivity status
- **Request Optimization**: Efficient pagination and caching strategies

## Testing Strategy

### Comprehensive Test Coverage (76 test files)
The application includes extensive testing across all layers:

#### Unit Tests
- **Use Case Testing**: All 13 use cases have dedicated test coverage
- **Utility Testing**: Password validation, network utilities, data mappers
- **Repository Testing**: Mock-based testing for data layer
- **ViewModel Testing**: State management and business logic validation

#### Test Categories
- **Authentication Tests**: Login, registration, logout, user validation
- **Posts Management Tests**: CRUD operations, pagination, search functionality
- **Favorites Tests**: Toggle operations, persistence, user association
- **Utility Tests**: Password hashing, network status, data transformations

#### Testing Tools & Frameworks
- **Mockito**: Mocking external dependencies
- **Coroutines Test**: Asynchronous code testing
- **JUnit**: Standard unit testing framework
- **Test Doubles**: Comprehensive mocking strategy

### Test Structure
```
app/src/test/java/dev/sadakat/technonexttest/
├── domain/usecase/
│   ├── auth/                    # Authentication use case tests
│   └── posts/                   # Posts use case tests
└── util/                        # Utility tests
```

## Assumptions & Limitations

### API Assumptions
- **External Dependency**: Relies on JSONPlaceholder API availability
- **Data Consistency**: API data structure remains consistent
- **No Authentication**: Public API without authentication requirements
- **Rate Limiting**: No explicit rate limiting implemented

### Data Management
- **Local Storage**: Assumes sufficient device storage for caching
- **Database Migration**: No migration strategies implemented for schema changes
- **Data Synchronization**: Basic sync without conflict resolution
- **User Isolation**: Single user per device assumption

### Network & Connectivity
- **Internet Dependency**: Core functionality requires internet connectivity
- **API Reliability**: No fallback for API service unavailability
- **Timeout Handling**: Standard timeout configurations without customization
- **Retry Logic**: Basic retry mechanisms without exponential backoff

### Security Considerations
- **Local Security**: Uses Android Security Crypto for preferences encryption
- **API Security**: No API key authentication implemented
- **Data Transmission**: HTTPS assumed for API endpoints
- **Local Attack Vectors**: Standard Android security model reliance

### Performance Limitations
- **Memory Usage**: No explicit memory management for large datasets
- **Image Handling**: No image caching or optimization (text-only posts)
- **Background Processing**: Limited background sync capabilities
- **Battery Optimization**: No specific battery usage optimizations

### Platform Constraints
- **Android Version**: Minimum API 24 (Android 7.0)
- **Device Compatibility**: No tablet-specific optimizations
- **Accessibility**: Basic accessibility support
- **Internationalization**: Limited to English language

## Key Insights

### Architecture Excellence
1. **Clean Architecture**: Proper separation of concerns enables maintainable and testable code
2. **Dependency Injection**: Hilt provides compile-time safety and reduces boilerplate
3. **Reactive Programming**: Flow and coroutines enable responsive UI and efficient data handling
4. **Modern UI**: Jetpack Compose provides declarative and performant UI development

### Development Best Practices
1. **Comprehensive Testing**: 76 test files ensure robust application behavior
2. **Type Safety**: Kotlin's type system prevents common runtime errors
3. **Code Organization**: Modular structure enhances team collaboration
4. **Performance Optimization**: Paging 3 and Room enable efficient large dataset handling

### Technical Decisions
1. **Offline-First Architecture**: Local database as primary data source ensures app usability
2. **MVVM Implementation**: Clear separation between UI and business logic
3. **Repository Pattern**: Abstract data access enables easy testing and maintenance
4. **Use Case Pattern**: Encapsulates business logic for reusability and testing

### Scalability Considerations
1. **Modular DI**: Easy to extend with new features and dependencies
2. **Use Case Architecture**: Business logic can be easily shared across features
3. **Repository Abstraction**: Easy to swap data sources or add new ones
4. **Compose UI**: Scalable UI components with reusable design system

### Production Readiness
1. **Error Handling**: Comprehensive error states and user feedback
2. **Security**: Encrypted preferences and secure data handling
3. **Testing Coverage**: Extensive test suite ensures reliability
4. **Performance**: Optimized for memory usage and responsive UI

This README provides a comprehensive overview of the TechnoNext Test Android application, covering all aspects from setup to deployment, architecture decisions, and technical implementation details.