
## JPX (ANDROID PROXY APP)
## Description
JPX Proxy App is a tool that acts as a local proxy server, allowing users to intercept, modify, and forward HTTP/2 requests and responses. It helps to analyse web applications requests and responses and to find potential Vulnerabilities in web sites . It features a user-friendly interface for viewing and modifying requests and responses, with support for features like http/https/http2 traffic interception, request/response manipulation, and much more.

## Features
- Intercept and modify http requests and responses
- Support for http/https/http2 interception
- View raw and modified network traffic in real-time
- Send and resend custom requests (Repeater functionality)
- UI components to display requests in a structured format

## Installation 
- Download the app from the release and install
- Download kiwi browser and use foxyproxy extension to setup proxy
- Open the proxy app and allow storage permission
- It will generate a  CA certificate and saves it to the path /DOCUMENTS/JPX
- Open settings and search for certificate click on install ca certificate and select the generated ca certificate
- Keep the proxy in background or use as popup window when browser
- The requests responses will appear on the proxy app

### Clone the Repository
```bash
git clone https://github.com/jithender2/JPX.git
```

### Import into Android Studio
1. Open Android Studio.
2. Click on `File` > `Open` and select the cloned repository directory.
3. Android Studio will automatically sync the project with Gradle and download any necessary dependencies.

## CodeAssist (In android) 
1. Create a project in Code assistant with package com.proxy
2. Copy the downloaded source code in the the project
3. Open library manager amd click import from gradle
4. Build the app



## Usage
1. **Start Proxy Server**: Open the app and start the proxy server.
2. **Modify Requests**: Use the `EditText` field to modify intercepted HTTP/2 requests.
3. **View Requests**: View all intercepted requests in an `ExpandableListView`.
4. **Send Custom Requests**: Modify and resend requests.
5. **SSL Interception**: View encrypted traffic in plaintext.
6. **Repeater**: Resend previously intercepted requests.




## Contributing

1. **Fork the Repository**.
2. **Clone Your Fork**.
   ```bash
   git clone https://github.com/jithender2/JPX.git
   ```
3. **Create a Branch**.
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. **Make Changes & Commit**.
   ```bash
   git commit -am "Add feature"
   ```
5. **Push Changes**.
   ```bash
   git push origin feature/your-feature-name
   ```
6. **Create a Pull Request** on GitHub.

## License
This project is licensed under the **BSD 2-Clause License** - see the [LICENSE](LICENSE) file for details.

## Contact
- **GitHub Issues**: Report bugs or suggest features.

## Credits

This project includes code from the **Cute-Proxy** project:

- **Original repository**: [https://github.com/hsiafan/cute-proxy](https://github.com/hsiafan/cute-proxy)
- **License**: BSD 2-Clause License  
- Some files in this project are directly taken from Cute-Proxy without modification.  
- Other parts of the code are based on Cute-Proxy but have been modified.  

For details, see the `NOTICE` file.
