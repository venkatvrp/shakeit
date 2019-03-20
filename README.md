# shakeit tool
The Shakedown Tool to perform shakedown on the server/application endpoints on the disconnected network.

HOW TO DEPLOY
-------------
1. Clone the project
2. Build the project using maven/gradle command e.g.,  mvn clean install or gradle assemble copy
3. Copy the shakeit/build/shakeit folder content to your local filesystem/server
4. Configure the server URLs and Application endpoints in the shakeit.shakedown.xml 
5. Edit the shakedown.bat with environment and target application name against which the shakedown needs to be performed.  
6. Environment & Application name specified in .bat file should match with the value specified in the shakeit.shakedown.xml.

HOW TO USE
----------
1. Double click the shakedown.bat
2. Output will be logged in the shakeit.log file.

FEATURES
--------
1. Can run from command prompt and any location.
2. N application endpoints can be configured.
3. N servers URLs can be configured.
4. Application endpoints and Server URLs are separated for scaling purpose.
5. Multiple URL levels can be configured.
6. Individual level statistics results provided in logs e.g., Total,Pass,Fail,Success Rate.
7. Displays number of servers scanned.
8. Displays the problematic URLs (HTTP and configured error messages expected on the page).
9. Displays the tool progress %
10. URLs are configurable in XML
