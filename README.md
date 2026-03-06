# WritersAssist

**WritersAssist** is a feature-rich, production-grade desktop screenwriting application built with **Java 17** and **JavaFX**. Designed specifically for screenwriters, authors, and creative teams, it delivers a powerful toolkit wrapped in a seamless, professional dark-themed UI. Through advanced networking integrations, WritersAssist aims to blend solo creative drafting with real-time multiparty collaboration.

---

## 🚀 Key Features

* **Professional Screenplay Editor**: Specialized editor with advanced formatting including automatic capitalization, dialogue detection, custom alignments, and distinct screenplay styles.
* **Character Management & Profiles**: Store character biographies, relationship webs, and attach visual inspiration directly into character profiles. 
* **Dynamic Inspiration Boards**: Organize ideas visually with an integrated mood board system.
* **Voice Memos**: Record, playback, and organize brief voice notations straight from the application to capture spontaneous ideas.
* **Spotify Integration Dashboard**: A built-in Spotify dashboard to listen to your specific custom playlists and curated tracks to fuel your creative process.
* **Real-time Collaboration**: Synchronize scripts across multiple clients using efficient Multicast strategies for collaborative writing sessions.
* **Cloud Storage & Data Persistence**: Secure SQLite-backed persistence, integrated with a remote server offering cloud savings and instantaneous callbacks.

---

## 🛠 Tech Stack & Architecture

* **Core Frameworks**: Java 17, JavaFX 21.0.9
* **Database Framework**: SQLite (JDBC)
* **Architecture Design**: Model-View-Controller (MVC)
* **Styling**: Pre-configured custom modular UI and fully reactive CSS styling (`style.css`).

### Networking & Advanced Java Integrations
This project thoroughly implements Advanced Java networking logic distributed across previous core learning Modules (Labs 5–8):
* **Lab 5 (InetAddress, URLConnection)**: Implemented in `NetworkingService` to securely fetch online resources and coordinate network data streams.
* **Lab 6 (Java Sockets & UDP Multicast)**: Orchestrated by the `CollaborationService` to continuously broadcast script state lines across a local network directly mimicking real-time collaboration.
* **Lab 7 (Java RMI)**: Employed within our robust `ScriptServerImpl` / `ScriptService` cloud architecture giving RMI clients centralized data hosting.
* **Lab 8 (RMI Callbacks)**: Supported through `ScriptCallback.java` natively allowing the remote server to dispatch cross-client update notifications instantly.

---

## 📂 Project Structure

```text
src/
└── com/writersassist/
    ├── controller/         # Request handling and control logic
    ├── db/                 # Database helper functions & schema building
    ├── lab5/               # Web Integration (URLConnection implementations)
    ├── lab6/               # P2P / Multicast Collaboration mechanisms
    ├── lab7/               # Java RMI Interfaces and Server implementation
    ├── lab8/               # Advanced Callbacks handling remote notifications
    ├── model/              # Data structural model classes (e.g., Script class)
    ├── ui/                 # Component styling and custom iconography (CSS/Images)
    └── view/               # Application UI/UX layers (Screens & Logic)
```

---

## ⚙️ Setup and Installation

### Prerequisites

1. **Java Development Kit (JDK) 17+**
2. **JavaFX SDK 21.0.9+**
3. **SQLite JDBC Driver** (included in `/lib`)
4. Unix-based CLI environment (macOS/Linux) to run the launch shell scripts.

### Build Configuration

Prior to initiating the build, you will need to map your local JavaFX SDK paths.
Open `run_writers.sh` and ensure the `JAVAFX_SDK` absolute path references your local system installation:
```bash
# Example configuration in run_writers.sh
JAVAFX_SDK="/Users/Unat/javafx-sdk-21.0.9"
```

---

## ▶️ How to Run

A compiled shell script handles artifact building, compiling, library attachment, and module mapping natively.

1. Ensure the run script has execution permissions:
   ```bash
   chmod +x run_writers.sh
   ```
2. Execute the script:
   ```bash
   ./run_writers.sh
   ```
3. **Select Execution Mode**: You will be prompted by an interactive CLI menu to choose what architecture components you wish to initialize.
   * `1) Cloud Backend` - Spin up the Java RMI Server (`ScriptServerImpl`) to host scripts remotely.
   * `2) WritersAssist App` - Launch the primary WritersAssist Desktop Application interface.
   * `3) Hybrid Launch` - Simultaneously deploy the Server backend and launch the Client App within 2 seconds of each other.

---
*Developed as the Final Project for Advanced Java Labs.*
