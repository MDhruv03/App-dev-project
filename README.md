# 📱 OpportunityHub - Internship, Job & Hackathon Finder

A modern, feature-rich Android application built with Material Design 3 that helps students and professionals discover, track, and prepare for career opportunities.

## ✨ Features

### 🏠 Home - Opportunity Discovery
- **Smart Recommendations** - AI-powered matching based on your skills and preferences
- **Advanced Filters** - Filter by type (Internship/Job/Hackathon), location, remote status, and more
- **Real-time Search** - Instantly find opportunities that match your interests
- **Pull-to-refresh** - Stay updated with the latest opportunities

### 💾 Saved Opportunities
- Bookmark opportunities for later review
- Quick access to your saved items
- Filter saved items by category

### 📊 Application Tracker
- Track application status (Applied, Interview, Rejected, Accepted)
- Set interview reminders
- Add notes and track important dates
- Visual status indicators

### 🤖 AI Interview Preparation
- **Domain-specific practice** (SDE, ML, Web, Android, HR)
- **Mock interview generator** with real-time feedback
- **Readiness score tracking** to monitor your progress
- **Topic-based practice** (DSA, OOPS, DBMS, System Design, Behavioral)

### 📈 Analytics Dashboard
- Visualize your application statistics
- Track success rate and interview conversion
- Monitor application timeline
- Recent activity feed

## 🏗️ Architecture

This app follows **MVVM (Model-View-ViewModel)** architecture:

```
app/
├── model/          # Data classes (Opportunity, User, Application, etc.)
├── view/           # Activities & Fragments (UI Layer)
├── viewmodel/      # Business logic
├── database/       # Room Database (DAOs & Database)
├── network/        # Retrofit API services
├── repository/     # Data management layer
├── adapter/        # RecyclerView adapters
├── util/           # Utility classes
└── ai/             # Interview evaluation logic
```

## 🎨 Design

- **Material Design 3** with custom theme
- **Light & Dark mode** support
- **Minimalist, elegant UI** with smooth animations
- **Card-based layouts** with proper elevation
- **Bottom navigation** for easy access to all features

## 🛠️ Technologies Used

- **Language**: Java
- **Build System**: Gradle
- **UI**: XML Layouts, Material Design 3
- **Database**: Room (SQLite)
- **Networking**: Retrofit + Gson
- **Async**: LiveData, ViewModel
- **Background Tasks**: WorkManager
- **Charts**: MPAndroidChart
- **Image Loading**: Glide

## 📋 Prerequisites

- **Android Studio** Electric Eel (2022.1.1) or later
- **JDK** 11 or later
- **Android SDK** 24 or higher (API Level 24+)
- **Gradle** 8.x

## 🚀 Setup Instructions

### 1. Install Android Studio

Download and install Android Studio from [developer.android.com](https://developer.android.com/studio)

### 2. Clone or Open the Project

1. Open Android Studio
2. Select **"Open an Existing Project"**
3. Navigate to this project folder and click **OK**

### 3. Configure Android SDK

The app will automatically prompt you to install missing SDK components. Accept and install:
- Android SDK Platform 34
- Android SDK Build-Tools
- Android Emulator (if you want to test without a physical device)

Alternatively, set the SDK location manually:
1. Go to **File > Project Structure > SDK Location**
2. Set the Android SDK location (usually `C:\Users\YourUsername\AppData\Local\Android\Sdk` on Windows)

### 4. Sync Gradle

1. Android Studio should automatically sync Gradle
2. If not, click on **File > Sync Project with Gradle Files**
3. Wait for dependencies to download

### 5. Run the App

#### Option A: Using an Emulator
1. Click **Tools > Device Manager**
2. Create a new virtual device (recommended: Pixel 6 with API 34)
3. Click the **Run** button (green play icon) or press **Shift + F10**
4. Select your emulator and wait for it to launch

#### Option B: Using a Physical Device
1. Enable **Developer Options** on your Android device:
   - Go to Settings > About Phone
   - Tap "Build Number" 7 times
2. Enable **USB Debugging** in Developer Options
3. Connect your device via USB
4. Click the **Run** button
5. Select your device from the list

## 📱 Minimum Requirements

- **minSdk**: 24 (Android 7.0 Nougat)
- **targetSdk**: 34 (Android 14)
- **RAM**: At least 8GB recommended for Android Studio
- **Storage**: 4GB for Android Studio + 2GB for SDK

## 🗂️ Project Structure

### Key Files

- `MainActivity.java` - Main entry point with bottom navigation
- `HomeFragment.java` - Displays opportunities with recommendations  
- `SavedFragment.java` - Shows bookmarked opportunities
- `TrackerFragment.java` - Application tracking dashboard
- `AIInterviewFragment.java` - Interview practice module
- `AnalyticsFragment.java` - Analytics and statistics

### Layouts

- `activity_main.xml` - Main activity with bottom navigation
- `fragment_home.xml` - Home screen layout
- `item_opportunity.xml` - Opportunity card (vertical)
- `item_opportunity_horizontal.xml` - Opportunity card (horizontal)

### Resources

- `res/values/colors.xml` - Custom color palette
- `res/values/themes.xml` - Material Design 3 theme
- `res/values/strings.xml` - All string resources
- `res/menu/bottom_nav_menu.xml` - Bottom navigation menu

## 🎯 Current Status

### ✅ Completed
- Material Design 3 UI with elegant minimalist design
- MVVM architecture setup
- All main fragments (Home, Saved, Tracker, AI Interview, Analytics)
- Bottom navigation
- Data models and database schema
- Sample data generator
- Opportunity cards with match percentage
- RecyclerView adapters

### 🚧 To Be Implemented
- API integration for real opportunities
- User authentication (Firebase)
- Resume parser
- AI interview evaluation logic
- Push notifications
- Analytics charts (MPAndroidChart)
- Search functionality
- Filter implementation

## 📝 Development Roadmap

### Phase 1 - Foundation ✅
- [x] Project setup with Gradle
- [x] Material Design 3 theme
- [x] MVVM architecture
- [x] Bottom navigation
- [x] Fragment structure

### Phase 2 - Core Features 🚧
- [ ] Implement ViewModels
- [ ] Connect Room Database
- [ ] API integration
- [ ] Search & filter logic
- [ ] Save/bookmark functionality

### Phase 3 - Advanced Features
- [ ] User authentication
- [ ] Resume parsing
- [ ] Recommendation algorithm
- [ ] Background sync (WorkManager)
- [ ] Push notifications

### Phase 4 - AI Interviewer
- [ ] Question bank
- [ ] Answer evaluation
- [ ] Feedback generation
- [ ] Progress tracking

### Phase 5 - Polish
- [ ] Analytics charts
- [ ] Animations
- [ ] Performance optimization
- [ ] Testing

## 🐛 Troubleshooting

### Gradle Sync Failed
- Make sure you have a stable internet connection
- Try **File > Invalidate Caches / Restart**
- Check if Android SDK is properly installed

### SDK Location Not Found
- Open `local.properties` file
- Set `sdk.dir` to your Android SDK path
- Example: `sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk`

### Build Errors
- Clean the project: **Build > Clean Project**
- Rebuild: **Build > Rebuild Project**
- Check if all dependencies are downloaded

### Emulator Won't Start
- Ensure hardware acceleration (Intel HAXM or Hyper-V) is enabled
- Try creating a new virtual device
- Increase emulator RAM allocation

## 📄 License

This project is created for educational purposes.

## 🤝 Contributing

This is an academic project. Feedback and suggestions are welcome!

## 📧 Contact

For questions or issues, please open an issue in the repository.

---

**Built with ❤️ using Material Design 3 and Modern Android Development practices**
