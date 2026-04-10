# InitiativeTracker

> Android app for tracking combat initiative in tabletop RPGs, focused on **fast turn management**, **visual character identification**, and **local persistence**.

![Platform](https://img.shields.io/badge/Platform-Android-green)
![Language](https://img.shields.io/badge/Language-Kotlin-purple)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-orange)
![Database](https://img.shields.io/badge/Persistence-Room-red)
![Status](https://img.shields.io/badge/Status-Active%20Development-brightgreen)

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

### Current
- Create, load, and delete rounds
- Add, edit, and remove characters
- Save data locally with Room
- Track initiative from highest to lowest
- Navigate turns forward and backward in a loop
- Round counter during combat
- Character portraits for quick recognition
- Active / inactive character handling
- Optional HP tracking
- Theme settings: light, dark, or system

### Planned
- Improved combat UI polish
- Better bottom sheet combat list
- Full-screen image preview
- Conditions / buffs / debuffs
- Concentration and duration tracking
- More combat edge-case handling
- Round export/import code for sharing between devices

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

## Architecture

The project follows a layered MVVM structure:

```text
UI (Compose Screens)
↓
ViewModel
↓
Repository
↓
Room Database / DAO

Project structure
com.dmc.initiativetracker
├── data
│   ├── local
│   │   ├── dao
│   │   ├── entity
│   │   ├── mapper
│   │   └── database
├── domain
│   └── model
├── repository
├── ui
│   ├── navigation
│   ├── screen
│   └── theme
├── viewmodel
└── di
Concepts Applied

This project was built to practice and apply modern Android development concepts:

MVVM architecture
Repository pattern
Local persistence with Room
Entity-to-domain model mapping
Reactive UI with Flow / StateFlow
Unidirectional state handling
Compose navigation
UI state modeling
Separation of concerns
Scalable folder organization
Technical Decisions

Some important design decisions in this project:

Initiative order remains fixed during combat
Turn tracking is based on currentCharacterId, not only list index
Only active characters participate in the combat loop
If the current character becomes inactive, the app does not auto-skip
Ending combat resets combat state instead of resuming later
HP is optional and can be displayed as ?
Data is stored locally only, without a backend
Design Priorities
Fast interaction during real play
Clear visual recognition of each character
Low-friction combat flow
Offline/local usability
Simple and maintainable architecture
Installation
Clone the repository
Open it in Android Studio
Sync Gradle
Run on an emulator or Android device
git clone https://github.com/HerreroAndre/InitiativeTracker.git
Author

Andrés Herrero
GitHub: HerreroAndre

This repository is part of my Android/Kotlin portfolio and learning path as a mobile developer.
