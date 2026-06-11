# InitiativeTracker

**Language / Idioma:** [English](README.md) · [Español](README_ES.md)

> A native Android initiative tracker for tabletop RPG combat, built with Kotlin, Jetpack Compose, Room, and MVVM. Designed for fast turn management, visual character recognition, and fully local persistence.

![Platform](https://img.shields.io/badge/Platform-Android-green)
![Language](https://img.shields.io/badge/Language-Kotlin-purple)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-orange)
![Database](https://img.shields.io/badge/Persistence-Room-red)
![Status](https://img.shields.io/badge/Status-Stable%20MVP-blue)

---

## Overview

InitiativeTracker is a mobile-first Android app designed to simplify combat flow in games like **D&D 5e (2014)**.

The project is built around a clear goal: make initiative tracking **fast, visual, and practical during real play sessions**.

### Core goals
- Quickly identify characters through images
- Manage initiative order in a loop
- Persist rounds and characters locally
- Keep the combat UI simple and readable
- Prioritize usability over unnecessary complexity

> This app is intentionally focused on **initiative tracking**, not on becoming a full rules engine or virtual tabletop.

---

## Features
- Create, load, and delete rounds
- Add, edit, and remove characters
- Save data locally with Room
- Navigate turns forward and backward in a loop
- Round counter during combat
- Character portraits for quick recognition
- Active / inactive character handling
- Optional HP tracking
- Light, dark, and system theme support
- Combat list with character overview
- Basic condition / status tracking
- System of exportation/importation of rounds through a sharable code

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| Platform | Android |
| UI | Jetpack Compose |
| Architecture | MVVM |
| Persistence | Room |
| Reactive State | Flow / StateFlow |
| Navigation | Navigation Compose |
| Build System | Gradle Kotlin DSL |

---

## Concepts Applied

This project was built to practice and apply modern Android development concepts, with a strong focus on clean structure, maintainability, and reactive UI patterns.

- **MVVM architecture**
- **Repository pattern**
- **Local persistence with Room**
- **Entity-to-domain model mapping**
- **Reactive UI with Flow / StateFlow**
- **Compose navigation**
- **Scalable folder organization**

---

## Technical Decisions

The project includes several intentional design decisions to keep combat flow stable, simple, and practical during play:

- Initiative order remains **fixed during combat**
- Turn tracking is based on **`currentCharacterId`**, not only list index
- Only **active** characters participate in the combat loop
- If the current character becomes inactive, the app **does not auto-skip**
- Ending combat **resets combat state** instead of resuming later
- HP is optional and can be displayed as **`?`**
- Data is stored **locally only**, without a backend

---

## Design Priorities

The app was planned around a few core product and UX priorities:

- **Fast interaction** during real play
- **Clear visual recognition** of each character
- **Low-friction combat flow**
- **Offline/local usability**
- **Simple and maintainable architecture**

---

## Author

**Andrés Herrero**  
[GitHub Profile](https://github.com/HerreroAndre)

This repository is part of my Android/Kotlin portfolio and reflects my learning path in modern Android development.

