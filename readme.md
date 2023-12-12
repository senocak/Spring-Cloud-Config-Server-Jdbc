# Spring Cloud Config Server As JDBC

<samp>
    <img src='https://img.shields.io/badge/kotlin-%237F52FF.svg?&style=flat&logo=kotlin&logoColor=white'>
    <img src='https://img.shields.io/badge/spring-%236DB33F.svg?style=flat&logo=spring&logoColor=white'>
    <img src='https://img.shields.io/badge/springboot-black?&style=flat&logo=springboot&logoColor=green'>

</samp>

### Screenshots

<img src="all.png" alt="all.png"/>
<img src="create.png" alt="create.png"/>
<img src="update.png" alt="update.png"/>
<img src="show.png" alt="show.png"/>
<img src="profile.png" alt="profile.png"/>
<img src="yml.png" alt="yml.png"/>
<img src="json.png" alt="json.png"/>
<img src="properties.png" alt="properties.png"/>


# Client
1. Run client application under ``client1`` folder
2. Update any value for
   1. application: `client1`
   2. profile: `dev`
3. Run the actuator refresh api to refresh the context in client app
   1. `curl -H "Content-Type: application/json" -d {} http://localhost:8081/actuator/refresh`
4. Check the logs in client application
5. Verify environment variable is changed

<img src="client-refresh.png" alt="client-refresh.png"/>