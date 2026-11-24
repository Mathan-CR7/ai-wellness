# 💚 AURA Wellness – AI-Powered Wellness Platform

## 📌 About the Project

AURA Wellness is an AI-powered wellness application that helps users monitor their daily physical activity and maintain a healthy lifestyle.

The application collects step data using Android Health Connect, stores activity information, and uses AI to provide personalized wellness recommendations.

It also supports team challenges, live leaderboards, and real-time activity tracking.

## ✨ Features

- **Step Tracking:** Monitor daily steps using Android Health Connect.
- **Activity Monitoring:** Track daily physical activity.
- **AI Wellness Coach:** Get personalized wellness recommendations.
- **Inactivity Detection:** Identify inactive periods and suggest simple exercises.
- **Team Challenges:** Participate in wellness challenges with other users.
- **Live Leaderboard:** View team activity rankings in real time.
- **Real-Time Updates:** Synchronize activity data using WebSocket.
- **User Authentication:** Secure access using JWT authentication.
- **Activity Analytics:** View daily, weekly, and monthly activity information.

## 🛠️ Technologies Used

| Category | Technologies |
|---|---|
| Backend | Java 21, Spring Boot |
| AI Integration | Spring AI, Gemini |
| Security | Spring Security, JWT |
| Database | MySQL |
| ORM | Spring Data JPA, Hibernate |
| Database Migration | Flyway |
| Real-Time Communication | WebSocket, STOMP |
| Android | Kotlin, Health Connect |
| Frontend | Web Dashboard |
| Build Tool | Gradle |
| Logging | Spring AOP |
| Version Control | Git, GitHub |

## 📂 Project Structure

```text
ai-wellness/
│
├── ai-wellness-service/   # Spring Boot backend
├── android-client/        # Android client
├── android-wellness-app/  # Android wellness application
├── web-dashboard/         # Web dashboard
│
├── gradle/                # Gradle configuration
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

## 🔄 Application Workflow

1. User logs into the application.
2. Android Health Connect provides activity data.
3. Mobile application synchronizes step data with the backend.
4. Backend stores activity information in MySQL.
5. Spring AI provides personalized wellness recommendations.
6. WebSocket sends real-time updates.
7. Users participate in team challenges and view leaderboards.

## 🧠 AI Wellness Features

- Analyze user activity information.
- Identify inactivity periods.
- Generate personalized exercise suggestions.
- Recommend simple stretching activities.
- Support healthier daily routines.

## 🚀 Future Enhancements

- Advanced wellness analytics.
- More personalized AI recommendations.
- Additional team wellness challenges.
- Improved activity visualizations.
- Advanced health and fitness integrations.

## 👨‍💻 Developer

**Mathan Prashath R.**

GitHub: [Mathan-CR7](https://github.com/Mathan-CR7)
