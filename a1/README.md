# REST API Testing

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

## Test Environment Setup 
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

## Testing the System with Python


### Todos Endpoint
```bash
# Todo testing
python .\random_todo_test_runner.py  
```
### Projects Endpoint
This implementation shows what happens if a system is not started so it requires starting the system
```bash
# Start the Server
java -jar .\runTodoManagerRestAPI-1.5.5.jar
# Project testing
python .\random_project_test_runner.py  
```