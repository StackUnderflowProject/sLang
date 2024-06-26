# sLang

sLang is a domain-specific language (DSL) designed to facilitate the conversion of code into GeoJSON format. This GeoJSON can then be displayed on various mapping platforms. The purpose of sLang is to provide an intuitive and efficient way to represent geographic data programmatically.

## Features

- Define geographic entities like points, lines, and polygons.
- Convert sLang code into GeoJSON format.
- Display GeoJSON data on maps using libraries like Leaflet or Mapbox.

## Getting Started

To get started with sLang, please refer to our [Wiki](https://github.com/StackUnderflowProject/slang/wiki).

## Installation

1. Clone the repository:
    ```sh
    git clone https://github.com/yourusername/slang.git
    ```
2. Navigate to the project directory:
    ```sh
    cd slang
    ```
3. Build the Kotlin parser:
    ```sh
    ./gradlew build
    ```

## Usage

1. Write your sLang code and save it to a `.txt` file (e.g., `map.txt`).
2. Run the sLang Kotlin parser to convert the `.txt` file to a `.json` file:
    ```sh
    java -jar slang-parser.jar map.txt map.json
    ```

## Contributing

We welcome contributions to sLang! Whether you want to add new features, fix bugs, or improve documentation, your help is appreciated. Please see our [contribution guidelines](https://github.com/yourusername/slang/wiki/Contributing) for more details.
