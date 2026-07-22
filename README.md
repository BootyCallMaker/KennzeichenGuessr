# License Plate Guessing Game

An interactive, web-based geography guessing game. The backend is built with Spring Boot, JPA, and PostgreSQL, and the frontend is a dark-mode single-page application utilizing Leaflet.js maps.

## Game Concept
1. When a round begins, the backend selects a random German city/region.
2. The backend dynamically calls the **Nominatim REST API** to resolve the city's exact latitude and longitude.
3. The backend returns these coordinates (and a secure round UUID) to the frontend. No city name or license plate code is sent to prevent client-side inspection cheating.
4. The frontend centers a **Leaflet.js** map on the coordinates.
5. The player inspects the map and submits a guess for the region's license plate prefix (e.g. `B` for Berlin, `M` for Munich, `HH` for Hamburg).
6. The backend validates the guess, stores the round status as resolved, and returns the result (correct/incorrect, correct answer, city name) to be displayed on the frontend.

## Prerequisites
- [Docker](https://www.docker.com/) (to run the PostgreSQL database container)
- [Java Development Kit (JDK) 17](https://adoptium.net/) or higher
- An IDE (IntelliJ IDEA, Eclipse, or VS Code with Java Extension Pack) or Maven

## How to Run

You can choose to run the application either completely inside **Docker Desktop** (recommended for a clean setup with a single click in your browser) or **locally on your machine** (using your IDE).

### Option A: Running in Docker Desktop (Recommended)

This method packages the backend and database into containers, letting you manage and launch the app directly from Docker Desktop.

1. **Compile the Backend**:
   Generate the package on your host machine using the Maven wrapper:
   * **Windows (PowerShell)**:
     ```powershell
     $env:JAVA_HOME="C:\Program Files\Zulu\zulu-21"  # Set if JAVA_HOME is not in environment
     .\mvnw.cmd package -DskipTests
     ```
   * **macOS / Linux**:
     ```bash
     ./mvnw package -DskipTests
     ```

2. **Start the Docker Stack**:
   Build and start the container services (database + backend app):
   ```bash
   docker compose up -d --build
   ```

3. **Open the Game**:
   * Open the **Docker Desktop** application.
   * Under the **Containers** tab, expand the `kennzeichenguessr` group.
   * Click the blue **`8080:8080`** port link next to `license_plate_game_container` (or manually navigate to `http://localhost:8080/index.html` in your browser).

---

### Option B: Running Locally (Traditional)

1. **Start the Database Only**:
   Start only the PostgreSQL database container from the root directory:
   ```bash
   docker compose up -d db
   ```

2. **Run the Spring Boot Application**:
   You can run the application directly from your IDE of choice:
   * **IntelliJ IDEA**: Open the folder as a project, wait for dependencies to sync, and run the `LicensePlateGameApplication` main class.
   * **VS Code**: Open the folder, ensure the Java Extension Pack is active, and press `F5`.
   * **Terminal (Maven Wrapper)**:
     * **Windows**: `.\mvnw.cmd spring-boot:run`
     * **macOS / Linux**: `./mvnw spring-boot:run`

3. **Open the Frontend**:
   Navigate to:
   ```
   http://localhost:8080/index.html
   ```

---

## Running the Tests

To run unit and integration tests (which use an in-memory H2 database, requiring no active Docker container):
* **IDE**: Run tests from `src/test/java/com/game/licenseplate/GameControllerTest.java`.
* **Terminal**:
  * **Windows**: `.\mvnw.cmd test`
  * **macOS / Linux**: `./mvnw test`

