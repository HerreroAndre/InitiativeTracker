# InitiativeTracker

> Aplicación Android para gestionar la iniciativa en combates de juegos de rol de mesa, enfocada en **manejo rápido de turnos**, **identificación visual de personajes** y **persistencia local**.

![Platform](https://img.shields.io/badge/Platform-Android-green)
![Language](https://img.shields.io/badge/Language-Kotlin-purple)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-orange)
![Database](https://img.shields.io/badge/Persistence-Room-red)
![Status](https://img.shields.io/badge/Status-Active%20Development-brightgreen)

---

## Descripción general

InitiativeTracker es una aplicación Android pensada para simplificar el flujo de combate en juegos como **D&D 5e (2014)**.

El proyecto fue diseñado alrededor de un objetivo claro: hacer que el seguimiento de iniciativa sea **rápido, visual y práctico durante sesiones reales de juego**.

### Objetivos principales
- Identificar rápidamente a los personajes mediante imágenes
- Gestionar el orden de iniciativa en loop
- Guardar rondas y personajes de forma local
- Mantener una interfaz de combate simple y fácil de leer
- Priorizar usabilidad por encima de complejidad innecesaria

> Esta app está enfocada intencionalmente en el **seguimiento de iniciativa**, no en convertirse en un motor completo de reglas o en un tabletop virtual.

---

## Funcionalidades

### Actuales
- Crear, cargar y eliminar rondas
- Agregar, editar y eliminar personajes
- Guardar datos localmente con Room
- Gestionar iniciativa de mayor a menor
- Navegar turnos hacia adelante y hacia atrás en loop
- Contador de rondas durante el combate
- Retratos de personajes para reconocimiento rápido
- Manejo de personajes activos / inactivos
- Seguimiento opcional de HP
- Ajustes de tema: claro, oscuro o sistema

### Planeamiento
- Mejoras visuales en la interfaz de combate
- Mejoras en la bottom sheet de combate
- Vista previa de imagen en pantalla completa
- Condiciones / buffs / debuffs
- Concentración y seguimiento de duración
- Mejor manejo de edge cases en combate
- Sistema de exportación/importación de rondas mediante código compartible

---

## Stack tecnológico

| Categoría | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| Plataforma | Android |
| UI | Jetpack Compose |
| Arquitectura | MVVM |
| Persistencia | Room |
| Estado reactivo | Flow / StateFlow |
| Navegación | Navigation Compose |
| Sistema de build | Gradle Kotlin DSL |

---


## Conceptos aplicados

Este proyecto fue desarrollado para practicar y aplicar conceptos modernos de desarrollo Android, con un fuerte enfoque en estructura limpia, mantenibilidad y patrones de UI reactiva.

- **Arquitectura MVVM**
- **Patrón Repository**
- **Persistencia local con Room**
- **Mapeo de entidades a modelos de dominio**
- **UI reactiva con Flow / StateFlow**
- **Manejo unidireccional del estado**
- **Navegación con Compose**
- **Modelado de estado de UI**
- **Separación de responsabilidades**
- **Organización escalable de carpetas**

---

## Decisiones técnicas

El proyecto incluye varias decisiones de diseño intencionales para mantener el flujo de combate estable, simple y práctico durante la partida:

- El orden de iniciativa permanece **fijo durante el combate**
- El turno actual se rastrea mediante **`currentCharacterId`**, no solo por índice de lista
- Solo los personajes **activos** participan del loop de combate
- Si el personaje actual pasa a inactivo, la app no lo **salta automáticamente y pasa al siguiente**
- Terminar el combate **reinicia el estado** en lugar de reanudarlo después
- El HP es opcional y puede mostrarse como **`?`**
- Los datos se almacenan solo de **forma local**, sin backend

---

## Prioridades de diseño

La app fue pensada alrededor de algunas prioridades centrales de producto y UX:

- **Interacción rápida** durante partidas reales
- **Reconocimiento visual claro de cada personaje**
- **Flujo de combate con baja fricción**
- **Uso local / offline**
- **Arquitectura simple y mantenible**

---

## Instalación

Para ejecutar el proyecto localmente:

1.Clonar el repositorio
2.Abrirlo en Android Studio
3.Sincronizar Gradle
4.Ejecutarlo en un emulador o dispositivo Android

```bash
git clone https://github.com/HerreroAndre/InitiativeTracker.git

```

---

## Autor

**Andrés Herrero**  
[Perfil Github](https://github.com/HerreroAndre)

Este repositorio forma parte de mi portfolio de Android/Kotlin y refleja mi camino de aprendizaje en desarrollo Android moderno.
