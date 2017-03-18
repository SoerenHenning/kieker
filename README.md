# Kieker Monitoring Framework

The internal behavior of large-scale software systems cannot be determined on the
basis of static (e.g., source code) analysis alone. Kieker provides complementary
dynamic analysis capabilities, i.e., monitoring and analyzing a software system’s
runtime behavior — enabling application performance monitoring and architecture
discovery.

Detailed information about Kieker is provided at http://kieker-monitoring.net/

## Usage

Kieker releases (stable, nightly, etc.) can be downloaded via
http://kieker-monitoring.net/download/

This is the source code of the Kieker framework, hosted at
https://github.com/kieker-monitoring/kieker

## Development

Kieker uses Gradle as build tool. To import Kieker as an Eclipse project, first execute `./gradlew eclipse`. This will generate the necessary IDE files for Eclipse.

The source can be imported as an Eclipse project. - No!

Gradle is used as the build tool. A `build.gradle` file is provided.

Further instructions for developers are available at
https://kieker-monitoring.atlassian.net/wiki/display/DEV/
