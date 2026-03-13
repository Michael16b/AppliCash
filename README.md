# 💸 AppliCash

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?style=flat&logo=kotlin)
![KMP](https://img.shields.io/badge/Platform-KMP-orange.svg)
![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-green.svg?logo=jetpackcompose)

**AppliCash** is a multi-module financial application built with **Kotlin Multiplatform (KMP)**. The project is designed with a focus on shared logic across platforms and a native, modern UI for Android using **Jetpack Compose**.

---

## 🚀 Project Overview
AppliCash follows a **Clean Modular Architecture**, separating concerns into distinct layers to ensure high maintainability and scalability:

* **Data Layer**: Responsible for API communication, local databases, and data mapping.
* **Domain Layer**: Contains the core business logic, entities, and reusable Use Cases.
* **Presentation Layer**: Built using feature-based modules with **Jetpack Compose** for a reactive UI.

---

## 🛠️ Tech Stack
* **Core Logic**: Kotlin Multiplatform (KMP)
* **Android UI**: Jetpack Compose
* **Dependency Injection**: Koin / Hilt (depending on module)
* **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)
* **Testing & Automation**: Maestro (UI automation), JUnit, MockK
* **CI/CD**: GitHub Actions

---

## 📊 Test Strategy & Quality Report
Quality is integrated into every step of our development. We maintain a strict testing matrix to ensure the stability of the shared logic and the mobile interface.

### 🔗 Live Test & Tests Report
Our automated reports, including code coverage and test execution results, are published at:
👉 **[https://michael16b.github.io/AppliCash/](https://michael16b.github.io/AppliCash/)**

👉 **[https://github.com/Michael16b/AppliCash/pull/66#issuecomment-4058461290](https://github.com/Michael16b/AppliCash/pull/66#issuecomment-4058461290)**

### 🧪 Coverage Matrix
| Layer | Test Type | Tools |
| :--- | :--- | :--- |
| **Domain** | Unit Testing | JUnit 5 / Kotlin Test |
| **Data** | Integration Testing | MockK / Flow |
| **Presentation** | UI Testing | Compose Testing Library |
| **End-to-End** | Automation | Maestro (Nightly UI runs) |

### 🤖 CI/CD Integration
* **Automated Runs**: Every Pull Request triggers a full suite of unit and integration tests.
* **Artifacts**: HTML coverage reports are automatically generated and deployed to GitHub Pages.
* **Naming Conventions**: We use descriptive naming (e.g., `Given_When_Then`) to ensure tests serve as living documentation.

---

## 👥 The Team
* **Michaël**
* **Maxence**
* **David**

---

## 🚦 Getting Started
1.  **Clone the repository**:
    ```bash
    git clone [https://github.com/Michael16b/AppliCash.git](https://github.com/Michael16b/AppliCash.git)
    ```
2.  **Open in Android Studio**: Use the latest stable version of Android Studio.
3.  **Run Tests locally**:
    ```bash
    ./gradlew test
    ```
4.  **Install on device**:
    ```bash
    ./gradlew installDebug
    ```

---
*For a more detailed breakdown of our testing infrastructure, please check [TESTING.md](./TESTING.md).*
