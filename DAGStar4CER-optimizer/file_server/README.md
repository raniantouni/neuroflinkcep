# File storage module

Python file server used to store and retrieve files.

Authentication layer requires valid credentials be sent at every request.

There is currently no folder structure implemented, every file is placed in the same root folder.

## Deployment and Dependencies

Add ALL necessary dependencies in the requirements.txt file.

Add the entire source code in the app folder.

Primary entrypoint at app.py main method (don't rename the module).

## Content mapping

Contents of these folders are visible to the python runtime.

    ./app -> /app
    ./volume1 -> /usr/share/volume1  [BOUND VOLUME]
    ./logs -> /usr/share/logs  [BOUND VOLUME]

## Instructions

The following paths are available:

    /                           GET             Server status
    /files                      GET             List of files
    /files/:file_name           GET             Download file contents by providing file name
    /files/exists/:file_name    GET             Checks if file exists by providing file name
    /files/:file_name           DELETE          Delete file by providing file name
    /files                      POST            Upload file (multipart upload)

The server response body has the following format (some fields may be absent):

```json
{
  "message": "example message",
  "error": "example error"
}
```

Status code are always set to 200 on success.

## Examples

A file with contents of "OK" and name "_test.txt" will always be available in the server.

    # Server status
    curl -u infore_user:infore_pass 192.168.1.200:20003
    
    # Get a list of files
    curl -u infore_user:infore_pass 192.168.1.200:20003/files
    
    # Retrieve a file
    curl -u infore_user:infore_pass 192.168.1.200:20003/files/_test.txt

    # Check if file exists
    curl -u infore_user:infore_pass 192.168.1.200:20003/files/exists/_test.txt

    # Upload a file
    curl -u infore_user:infore_pass -X POST -F "file=@_test.txt" 192.168.1.200:20003/files
    
    # Delete a file
    curl -u infore_user:infore_pass -X DELETE 192.168.1.200:20003/files/_test.txt
