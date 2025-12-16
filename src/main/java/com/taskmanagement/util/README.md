# Utility Layer

## 📋 Status: NOT IMPLEMENTED

**Location:** `src/main/java/com/taskmanagement/util/`

**Current State:** Empty folder - no utility classes yet

---

## 🔲 Planned Utilities

This folder is reserved for future helper classes and common utilities:

### Phase 1: Date/Time Utilities
- `DateTimeUtil.java` - Date formatting, calculations, timezone handling
- `TimeRangeUtil.java` - Working hours, date ranges, durations

### Phase 2: String Utilities
- `StringUtil.java` - Validation, sanitization, formatting
- `SlugGenerator.java` - Generate URL-friendly slugs from titles

### Phase 3: Security Utilities
- `SecurityUtil.java` - Get current user, check permissions
- `PasswordGenerator.java` - Generate secure random passwords

### Phase 4: Validation Utilities
- `EmailValidator.java` - Advanced email validation
- `PhoneNumberValidator.java` - Phone number formatting/validation

---

## 📝 Current Approach

**No utilities needed yet** - The MVP phase uses:
- Java standard library (LocalDateTime, String methods)
- Lombok annotations (@Data, @Builder)
- Spring Boot utilities (StringUtils)
- Bean Validation annotations (@NotBlank, @Size)

Utilities will be added when patterns emerge and code duplication needs to be eliminated.

---

## 🔮 Future Implementation

When utilities are needed, this folder will contain:

```
util/
├── DateTimeUtil.java
├── StringUtil.java
├── SecurityUtil.java
└── README.md
```

**Design principle:** YAGNI (You Aren't Gonna Need It) - only create utilities when there's clear duplication across multiple places.

---

**Last Updated:** December 15, 2025  
**Version:** 0.5.0 - MVP Phase  
**Status:** Placeholder - awaiting utility implementations
