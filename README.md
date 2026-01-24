# 🚢 Hundir la Flota (Battleship)

Juego multijugador de Hundir la Flota (Battleship) desarrollado con Kotlin Multiplatform y Compose for Desktop.

## 📋 Descripción

Implementación completa del clásico juego de estrategia naval con dos modos de juego:
- **PVP (Player vs Player)**: Partidas multijugador en red
- **PVE (Player vs AI)**: Partidas contra inteligencia artificial

El proyecto incluye servidor dedicado, protocolo de comunicación JSON sobre TCP, sistema de records/estadísticas, y una interfaz gráfica moderna con Jetpack Compose.

## ✨ Características

### Funcionalidades de Juego
- ✅ **Tablero 10x10** con flota clásica de 5 barcos (17 casillas totales)
- ✅ **Fase de colocación** interactiva con validación de reglas
- ✅ **Fase de batalla** con sistema de turnos
- ✅ **Validación servidor-side** (anticheat)
- ✅ **IA con dos niveles de dificultad** (EASY, NORMAL)
- ✅ **Temporizador de turno** visual (60 segundos por defecto)
- ✅ **Historial de movimientos** en tiempo real
- ✅ **Detección de desconexión** de rivales
- ✅ **Game Over automático** con razones (flota hundida, desconexión, timeout)

### Sistema de Records
- ✅ **Persistencia JSON** con respaldos automáticos
- ✅ **Estadísticas por modo** (PVP y PVE separados)
- ✅ **Métricas avanzadas**: victorias, derrotas, rachas, precisión, victoria rápida
- ✅ **Top 10 jugadores** con ranking visual
- ✅ **Win rate calculado** con indicadores de color

### Arquitectura
- ✅ **Cliente-Servidor TCP** con protocolo JSON
- ✅ **Arquitectura limpia** (Domain, UseCase, Protocol, UI)
- ✅ **Coroutines** para concurrencia
- ✅ **Serialización kotlinx** para protocolo
- ✅ **Compose for Desktop** para UI reactiva

## 🚀 Instalación y Ejecución

### Requisitos

- **JDK 17** o superior
- **Gradle 8.x** (incluido wrapper)
- **Sistema operativo**: Windows, macOS, Linux

### Compilación

```bash
# Windows
.\gradlew.bat :composeApp:build

# macOS/Linux
./gradlew :composeApp:build
```

### Ejecución

#### Servidor

```bash
# Windows
.\gradlew.bat :composeApp:run -PmainClass=com.mario.hlf.ServerMainKt

# macOS/Linux
./gradlew :composeApp:run -PmainClass=com.mario.hlf.ServerMainKt
```

El servidor se iniciará en:
- **Host**: 127.0.0.1
- **Puerto**: 5678

#### Cliente

```bash
# Windows
.\gradlew.bat :composeApp:run

# macOS/Linux
./gradlew :composeApp:run
```

### Configuración del Servidor

Editar `server.properties` (se crea automáticamente en la primera ejecución):

```properties
# Configuración de red
host=127.0.0.1
port=5678
maxClients=10

# Configuración de partida
boardSize=10
turnSeconds=60
bestOf=3

# Dificultad de IA
aiDifficulty=NORMAL  # EASY, NORMAL, HARD
```

## 🎮 Guía de Uso

### Menú Principal

1. **Conectar al servidor**
   - Introducir host y puerto (por defecto: 127.0.0.1:5678)
   - Introducir nombre de jugador
   - Seleccionar modo: PVP o PVE

2. **Ver Records**
   - Top 10 jugadores por modo
   - Estadísticas detalladas: victorias, derrotas, rachas, precisión

3. **Ver Configuración**
   - Configuración actual del servidor
   - Parámetros de partida

### Lobby

- **PVP**: Esperar a que se complete la sala (2 jugadores)
  - P1 (anfitrión) puede iniciar la partida
  - P2 debe esperar a que P1 inicie

- **PVE**: La IA está lista inmediatamente
  - Puedes iniciar la partida cuando quieras

### Fase de Colocación

1. Seleccionar tipo de barco del panel lateral
2. Elegir orientación (Horizontal/Vertical)
3. Hacer clic en el tablero para colocar
4. Repetir hasta colocar los 5 barcos:
   - 🚢 Portaaviones (5 casillas)
   - ⚓ Acorazado (4 casillas)
   - 🛥️ Crucero (3 casillas)
   - 🚤 Submarino (3 casillas)
   - ⛵ Destructor (2 casillas)

**Reglas de colocación:**
- No se pueden solapar barcos
- No se pueden salir del tablero
- No pueden estar adyacentes (opcional según configuración)

### Fase de Batalla

1. **Tu turno**:
   - Indicador "🎯 TU TURNO" en verde
   - Temporizador de cuenta regresiva (⏱️ Xs)
   - Hacer clic en el radar enemigo para disparar
   - El disparo cambia el turno automáticamente

2. **Turno del rival**:
   - Indicador "⏳ Turno del Rival" en gris
   - Esperar a que disparen

3. **Visualización**:
   - **Mi Flota (izquierda)**: Tus barcos y disparos enemigos
     - 🟦 Agua
     - 🟩 Barco intacto
     - 🔴 Barco impactado
     - ⚫ Agua disparada

   - **Radar Enemigo (centro)**: Tus disparos
     - ⬜ Sin explorar
     - ⚫ Agua
     - 🔴 Impacto

   - **Historial (derecha)**: Últimos 10 movimientos
     - 💥 Impactos
     - 💨 Fallos

### Fin de Partida

**Victoria** si:
- Hundes toda la flota enemiga
- El rival se desconecta

**Derrota** si:
- Tu flota es hundida
- Te desconectas
- Se agota tu tiempo de turno (implementación futura)

## 🏗️ Arquitectura del Proyecto

```
composeApp/src/jvmMain/kotlin/com/mario/hlf/
├── domain/              # Lógica de negocio (reglas del juego)
│   ├── model/          # Entidades: Ship, Board, Coordinate, etc.
│   ├── rules/          # Game.kt (máquina de estados del juego)
│   └── usecase/        # Casos de uso: PlaceShip, Shoot, GetGameState
│
├── protocol/           # Definición del protocolo JSON
│   ├── Dtos.kt         # DTOs serializables
│   ├── Messages.kt     # Mensajes del protocolo
│   ├── GameStateDto.kt # Estado del juego para cliente
│   └── RequestMappers.kt # Conversiones Domain ↔ Protocol
│
├── server/             # Servidor TCP
│   ├── TcpGameServer.kt        # Servidor principal
│   ├── ClientSession.kt        # Gestión de sesiones
│   ├── MessageRouter.kt        # Enrutamiento de mensajes
│   ├── GameService.kt          # Servicio de partidas
│   ├── RecordsManager.kt       # Sistema de estadísticas
│   └── ai/
│       └── AIPlayer.kt         # Lógica de IA
│
├── client/             # Cliente de red
│   └── TcpGameClient.kt
│
└── ui/                 # Interfaz gráfica (Compose)
    ├── GameController.kt       # Controlador principal
    ├── GameUiState.kt         # Estados de UI
    ├── components/            # Componentes reutilizables
    │   └── BoardGrid.kt
    └── screens/               # Pantallas
        ├── MainMenuScreen.kt
        ├── LobbyScreen.kt
        ├── PlacementScreen.kt
        ├── BattleScreen.kt
        ├── RecordsScreen.kt
        └── SettingsScreen.kt
```

### Flujo de Datos

```
[Cliente] <--JSON/TCP--> [Servidor] <--> [GameService] <--> [Domain]
    ↓                                                            ↑
[GameController]                                                 |
    ↓                                                            |
[GameUiState]                                             [Validaciones]
    ↓
[Compose UI]
```

## 📡 Protocolo de Comunicación

### Formato de Mensajes

Todos los mensajes siguen el formato:

```json
{
  "id": "uuid-mensaje",
  "timestamp": "2026-01-24T12:00:00Z",
  "payload": { ... }
}
```

### Handshake (Cliente → Servidor)

```json
{
  "type": "Hello",
  "playerName": "Mario",
  "mode": "PVP"  // o "PVE"
}
```

**Respuesta (Servidor → Cliente):**

```json
{
  "type": "Welcome",
  "gameId": "game-uuid",
  "roomId": "room-uuid",
  "playerId": "P1",  // o "P2"
  "roomStatus": "WAITING",  // o "READY"
  "serverConfig": {
    "host": "127.0.0.1",
    "port": 5678,
    "maxClients": 10,
    "boardSize": 10,
    "turnSeconds": 60,
    "bestOf": 3,
    "aiDifficulty": "NORMAL"
  },
  "records": { ... }
}
```

### Iniciar Partida (P1 → Servidor)

```json
{
  "type": "StartGame",
  "boardSize": 10,
  "allowAdjacency": false
}
```

### Colocar Barco (Cliente → Servidor)

```json
{
  "type": "PlaceShip",
  "player": "P1",
  "row": 0,
  "col": 0,
  "ship": "CARRIER",
  "orientation": "HORIZONTAL"
}
```

### Disparar (Cliente → Servidor)

```json
{
  "type": "Shoot",
  "player": "P1",
  "row": 5,
  "col": 3
}
```

### Estado del Juego (Servidor → Cliente)

```json
{
  "type": "GameState",
  "state": {
    "phase": "BATTLE",  // PLACEMENT, BATTLE, OVER
    "currentTurn": "P1",
    "winner": null,
    "self": {
      "size": 10,
      "cells": [
        ["SHIP", "SHIP", "HIT", ...],
        ...
      ]
    },
    "opponent": {
      "size": 10,
      "cells": [
        ["UNKNOWN", "MISS", "HIT", ...],
        ...
      ]
    }
  }
}
```

### Game Over (Servidor → Todos)

```json
{
  "type": "GameOver",
  "winner": "P1",
  "reason": "ALL_SHIPS_SUNK"  // OPPONENT_DISCONNECTED, TIMEOUT
}
```

### Errores (Servidor → Cliente)

```json
{
  "type": "Error",
  "code": "INVALID_MOVE",
  "message": "Coordenada fuera del tablero"
}
```

## 🧠 Sistema de IA

### Niveles de Dificultad

**EASY** (Aleatoria):
- Dispara a coordenadas aleatorias no exploradas
- Sin estrategia

**NORMAL** (Inteligente):
- Dispara aleatoriamente hasta encontrar un barco
- Tras un impacto, dispara en las 4 direcciones adyacentes
- Continúa en la misma dirección si hay múltiples impactos consecutivos
- Vuelve a modo aleatorio cuando hunde el barco

**HARD** (Avanzada - Futuro):
- Estrategia de paridad (tablero de ajedrez)
- Priorización de zonas por densidad de barcos
- Deducción de barcos restantes

### Implementación

La IA se ejecuta en el servidor y responde automáticamente después de cada disparo del jugador:

```kotlin
class AIPlayer(private val difficulty: AIDifficulty) {
    suspend fun makeMove(game: Game, aiPlayer: Game.Player): Coordinate {
        return when (difficulty) {
            AIDifficulty.EASY -> randomShot(game, aiPlayer)
            AIDifficulty.NORMAL -> intelligentShot(game, aiPlayer)
            AIDifficulty.HARD -> advancedShot(game, aiPlayer)
        }
    }
}
```

## 📊 Sistema de Records

### Estructura de Datos

Los records se almacenan en `records.json`:

```json
{
  "players": {
    "Mario": {
      "pvp": {
        "wins": 5,
        "losses": 3,
        "draws": 0,
        "bestStreak": 3,
        "currentStreak": 1,
        "bestAccuracy": 0.75,
        "avgAccuracy": 0.62,
        "fastestWinTurns": 18,
        "totalGames": 8,
        "totalShots": 120,
        "totalHits": 74
      },
      "pve": {
        "wins": 12,
        "losses": 4,
        ...
      },
      "lastUpdated": "2026-01-24T12:00:00Z"
    }
  },
  "meta": {
    "version": 1,
    "lastBackup": "2026-01-24T11:00:00Z"
  }
}
```

### Métricas Calculadas

- **Win Rate**: `(wins / totalGames) * 100`
- **Accuracy**: `(totalHits / totalShots) * 100`
- **Best Streak**: Máximo de victorias consecutivas
- **Current Streak**: Racha actual (resetea con derrota)
- **Fastest Win**: Mínimo de turnos para ganar

### Respaldos Automáticos

El sistema crea respaldos automáticos:
- Cada vez que se detecta corrupción en el archivo principal
- Formato: `records.json.backup.TIMESTAMP`

## 🧪 Testing

```bash
# Ejecutar tests unitarios
./gradlew :composeApp:test

# Ejecutar tests de integración
./gradlew :composeApp:integrationTest
```

## 🐛 Troubleshooting

### El servidor no inicia

**Problema**: `Address already in use`
**Solución**: Cambiar puerto en `server.properties` o matar proceso:
```bash
# Windows
netstat -ano | findstr :5678
taskkill /PID <PID> /F

# Linux/macOS
lsof -ti:5678 | xargs kill -9
```

### El cliente no se conecta

**Problema**: `Connection refused`
**Solución**:
1. Verificar que el servidor está ejecutándose
2. Comprobar host y puerto correctos
3. Revisar firewall

### La partida se congela

**Problema**: Sin respuesta del servidor
**Solución**:
1. Revisar logs del servidor
2. Comprobar conexión de red
3. Reiniciar servidor y cliente

### Records corruptos

**Problema**: `JsonDecodingException`
**Solución**: El sistema restaurará automáticamente desde backup. Si falla:
```bash
# Eliminar archivo corrupto (se regenerará)
rm records.json
```

## 📝 Roadmap

### Versión 1.1
- [ ] Sistema de rondas (best of 3/5)
- [ ] Timeout real de turno con penalización
- [ ] Modo espectador
- [ ] Chat integrado

### Versión 1.2
- [ ] IA nivel HARD con estrategia avanzada
- [ ] Sonidos y efectos visuales
- [ ] Animaciones de disparos y hundimientos
- [ ] Temas personalizables

### Versión 2.0
- [ ] Matchmaking automático
- [ ] Ladder/ranking global
- [ ] Replay de partidas
- [ ] Torneos

## 👥 Contribuciones

Este es un proyecto académico. Las contribuciones son bienvenidas:

1. Fork del repositorio
2. Crear branch de feature (`git checkout -b feature/NuevaCaracteristica`)
3. Commit de cambios (`git commit -m 'Añadir nueva característica'`)
4. Push al branch (`git push origin feature/NuevaCaracteristica`)
5. Abrir Pull Request

## 📄 Licencia

Proyecto académico - 2º DAM (2026)

## 🙏 Agradecimientos

- Kotlin Multiplatform Team
- JetBrains Compose Team
- Comunidad de Kotlin

## 📧 Contacto

Para reportar bugs o sugerencias, abrir un issue en el repositorio.

---

**Desarrollado con ❤️ usando Kotlin y Compose**
