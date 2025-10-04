# REST API Testing

## Execute the build
```bash
java -jar .\runTodoManagerRestAPI-1.5.5.jar
```

## Python Script
```bash
# Venv
python -m venv venv

# Activate/ Deactivate
# Linux
source venv/bin/activate
# Windows
.\venv\Scripts\Activate.ps1       
# Deactivate
deactivate

# Freeze req
pip freeze > requirements.txt

# Install
pip install -r requirements.txt

# Unit test in python

# Project testing
python project_api_unit_test.py -v
```