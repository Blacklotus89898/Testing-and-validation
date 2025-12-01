# REST API Performance Testing Framework

## Overview
This project is a lightweight, object-oriented Python framework designed to conduct **Non-Functional Performance Testing** on REST APIs. It measures execution time (latency) and system resource consumption (CPU and Memory) under varying load levels.

It currently supports testing `Todo` and `Project` entities but is designed to be easily extensible for other resources.

## Features
* **Automated Load Generation:** Spawns configurable numbers of objects (Create/Update/Delete cycles).
* **Resource Monitoring:** Uses a background daemon thread to monitor CPU % and RAM usage relative to a baseline.
* **Scalability Analysis:** Measures time complexity ($O(n)$) across different load levels (e.g., 10, 100, 1000 items).
* **Visual Reporting:** Includes a Jupyter Notebook for generating graphs and data tables.

## Setup Environment
```bash
# Windows
java --version
# java 21.0.2 2024-01-16 LTS
# Java(TM) SE Runtime Environment (build 21.0.2+13-LTS-58)
# Java HotSpot(TM) 64-Bit Server VM (build 21.0.2+13-LTS-58, mixed mode, sharing)
```
## Prerequisites
* Python 3.8+
* The target REST API running locally (default: `http://localhost:4567`)
```bash
# Start the Server
java -jar .\runTodoManagerRestAPI-1.5.5.jar
``` 

## Installation

1.  **Clone or Download** the repository.
2.  **Create a Virtual Environment** (Recommended):
    ```bash
    python -m venv venv
    # Windows:
    .\venv\Scripts\activate
    # Mac/Linux:
    source venv/bin/activate
    ```
3.  **Install Dependencies**:
    ```bash
    pip install requests psutil pandas matplotlib ipykernel
    ```

## Usage

### Option 1: Command Line (Text Output)
To run the standard test suite and see a summary table in the terminal:

```bash
python TestRunner.py
```

### Option 2: Jupyter Notebook (Visual Analysis)

To generate graphs and view detailed data frames:

1. Ensure your virtual environment is selected as the Kernel.

2. Open PerformanceAnalysis.ipynb in VS Code or Jupyter Lab.

3. Run all cells.