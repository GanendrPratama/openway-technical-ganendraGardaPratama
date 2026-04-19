# Gmail Test Automation

This project contains automated tests for Gmail using Java, Selenium, and TestNG.

## Test Documentation

The LaTeX test documentation is in `TestDocumentation.tex`. To compile:
```bash
pdflatex TestDocumentation.tex
```

## Project Structure

```
.
├── pom.xml                                    # Maven configuration
├── TestDocumentation.tex                     # Test documentation in LaTeX
├── src/test/java/com/qa/gmail/
│   └── GmailUnreadEmailTest.java            # Automated test
├── src/test/resources/
│   └── testng.xml                           # TestNG configuration
└── README.md
```

## Prerequisites

- Java 11+
- Maven 3.6+
- Chrome browser installed

## Running the Tests

1. Set environment variables for credentials:
```bash
export TEST_EMAIL="your_email@gmail.com"
export TEST_PASSWORD="your_password"
```

2. Or pass as system properties:
```bash
mvn test -Dtest.email=your_email@gmail.com -Dtest.password=your_password
```

3. Run tests:
```bash
mvn test
```

## Test Features

- Opens Chrome in a new window
- Navigates to https://mail.google.com/mail/
- Handles login with email and password
- Handles OTP/verification if required (allows manual entry)
- Retrieves and logs the title of the last unread email
- Uses explicit waits for dynamic content

## Configuration

- Timeout: 30 seconds (configurable in code)
- Chrome options: Maximized, notifications disabled
- Implicit wait: 10 seconds
- Page load timeout: 30 seconds