# 🚢 Hundir la Flota (Battleship)

Juego de Hundir la Flota multijugador con arquitectura cliente-servidor, desarrollado en Kotlin Multiplatform con Compose Desktop.

## 🎮 Características

- **Modo PvE**: Juega contra la IA
- **Modo PvP**: Juega contra otro jugador en red
- **Sistema de Records**: Guarda estadísticas de victorias/derrotas
- **Interfaz moderna**: Tema Matrix/militar con efectos visuales
- **Arquitectura cliente-servidor**: Servidor TCP para partidas multijugador

## 🎯 Flota disponible

- **Portaaviones** (5 casillas)
- **Acorazado** (4 casillas)
- **Crucero** (3 casillas)
- **Submarino** (3 casillas)
- **Destructor** (2 casillas)

**Total**: 5 barcos, 17 casillas

## 📦 Descarga Ejecutables

Los ejecutables compilados están disponibles en **[GitHub Releases](../../releases)**:

### Instalador MSI (Recomendado)
- `com.mario.hlf-1.0.0.msi` (~119 MB)
- Instalador completo para Windows
- Crea accesos directos en el menú inicio
- Se instala en `C:\Program Files\com.mario.hlf\`

### Versión Portable
- `com.mario.hlf-portable.zip` (~178 MB)
- No requiere instalación
- Ejecutable directo con runtime de Java incluido
- Ideal para testing y desarrollo

**Nota**: Los ejecutables NO están incluidos en el repositorio para mantenerlo ligero. Descárgalos desde la sección [Releases](../../releases).

## 🚀 Cómo jugar

### Modo PvE (Un jugador)
1. Ejecuta el juego
2. Selecciona "ENTRENAMIENTO" en el menú principal
3. Coloca tus naves en el tablero
4. ¡Comienza a hundir la flota enemiga!

### Modo PvP (Multijugador)

#### Ejecutar el servidor (desde IDE):
```kotlin
// Abre el proyecto en IntelliJ IDEA o Android Studio
// Ejecuta Main.kt
// El servidor escuchará en el puerto configurado
```

#### Conectar clientes:
1. **Cliente 1**: Ejecuta `dist/portable/com.mario.hlf.exe`
2. **Cliente 2**: Ejecuta otra instancia del ejecutable
3. En cada cliente:
   - Ve a "MULTIJUGADOR" en el menú
   - Ingresa la IP del servidor (localhost si es local)
   - Espera en el lobby a que ambos jugadores estén listos
   - Coloca tus naves
   - ¡Comienza la batalla!

## 🛠️ Desarrollo

### Requisitos
- JDK 17 o superior
- Kotlin 2.1.0
- Gradle 8.14.3

### Compilar el proyecto
```bash
./gradlew build
```

### Generar ejecutables
```bash
# Instalador MSI
./gradlew packageMsi

# Versión portable
./gradlew createDistributable

# Ambos
./gradlew packageDistributionForCurrentOS
```

Los ejecutables se generan en:
- MSI: `composeApp/build/compose/binaries/main/msi/`
- Portable: `composeApp/build/compose/binaries/main/app/com.mario.hlf/`

### Ejecutar desde IDE
```bash
./gradlew run
```

## 📁 Estructura del proyecto

```
crazy-noether/
├── composeApp/
│   └── src/
│       └── jvmMain/
│           ├── kotlin/com/mario/hlf/
│           │   ├── client/          # Cliente de red
│           │   ├── domain/          # Modelos de dominio
│           │   ├── protocol/        # Protocolo de comunicación
│           │   ├── server/          # Servidor TCP
│           │   ├── ui/              # Interfaz de usuario
│           │   │   ├── components/  # Componentes reutilizables
│           │   │   └── screens/     # Pantallas del juego
│           │   ├── App.kt           # Aplicación principal
│           │   └── Main.kt          # Punto de entrada
│           └── resources/
│               └── drawable/        # Recursos gráficos
└── dist/                            # Ejecutables distribuibles
    ├── com.mario.hlf-1.0.0.msi     # Instalador Windows
    └── portable/                    # Versión portable
        └── com.mario.hlf.exe
```

## 🎨 Assets pendientes

Actualmente, las imágenes de las naves tienen un factor de escala temporal (`scaleY = 2.2`) para mejorar su visualización. Se recomienda reemplazar los assets actuales por imágenes con las siguientes especificaciones:

### Dimensiones ideales para las naves:
- **Alto consistente**: ~120-130px para todas las naves
- **Anchos variables**:
  - Destructor (2 celdas): 140px × 120px
  - Cruiser (3 celdas): 210px × 120px
  - Battleship (4 celdas): 280px × 120px
  - Carrier (5 celdas): 350px × 120px

### Características técnicas:
- Formato: PNG con transparencia
- Vista: Superior (bird's eye view)
- Orientación: Horizontal
- Estilo: Naval/militar consistente con tema Matrix

## 📝 Notas técnicas

- **Protocolo de red**: TCP con serialización JSON
- **Gestión de estado**: MutableState de Compose
- **Detección de barcos**: Algoritmo BFS para identificar patrones
- **Sistema de records**: Almacenamiento en JSON local
- **Tema visual**: Matrix/militar con colores verde fluorescente (#00FF00)

## 🎯 Roadmap

- [ ] Mejorar assets visuales de las naves
- [ ] Añadir sonidos y música
- [ ] Implementar chat en partidas multijugador
- [ ] Añadir más variantes de juego
- [ ] Soporte para más de 2 jugadores

## 📜 Licencia

Este proyecto es de código abierto para fines educativos.

---

**Desarrollado con ❤️ usando Kotlin Multiplatform y Jetpack Compose**
