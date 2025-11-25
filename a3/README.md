# REST API Performance Testing

## Setup Environment
```bash
# Windows
java --version
# java 21.0.2 2024-01-16 LTS
# Java(TM) SE Runtime Environment (build 21.0.2+13-LTS-58)
# Java HotSpot(TM) 64-Bit Server VM (build 21.0.2+13-LTS-58, mixed mode, sharing)
```

## Running the Executable
```bash
# Assuming the jar file is in current directory
java -jar .\runTodoManagerRestAPI-1.5.5.jar
```

# Test Environment Setup 
```bash
# Make virtual environment on windows
python -m venv venv

# Activate/ Deactivate
# Linux
source venv/bin/activate
# Windows
.\venv\Scripts\Activate.ps1       
# Deactivate
deactivate

# Freeze req
# pip freeze > requirements.txt

# Install the requirements into venv
pip install -r requirements.txt
```

<!-- TODO: Catch errors and report at the end of testing, add the project and linking tests to it -->

### Running Tests

```bash
# Start the Server
java -jar .\runTodoManagerRestAPI-1.5.5.jar
# Performance testing
python .\TestRunner.py  
``` 