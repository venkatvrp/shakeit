# shakeit tool
The Shakedown Tool to perform shakedown on the server/application endpoints on the disconnected network.

HOW TO USE
----------
. Clone the project
. Build the project using maven/gradle command e.g.,  mvn clean install or gradle assemble copy
. Copy the shakeit/build/shakeit folder content to your local filesystem/server
. Configure the server URLs and Application endpoints in the shakeit.shakedown.xml 
. Edit the shakedown.bat with environment and target application name against which the shakedown needs to be performed.  
. Environment & Application name specified in .bat file should match with the value specified in the shakeit.shakedown.xml.
. Double click the shakedown.bat
. Output will be logged in the shakeit.log file.

FEATURES
--------
. Can run from command prompt and any location.
. N application endpoints can be configured.
. N servers URLs can be configured.
. Application endpoints and Server URLs are separated for scaling purpose.
. Multiple URL levels can be configured.
. Individual level statistics results provided in logs e.g., Total,Pass,Fail,Success Rate.
. Displays number of servers scanned.
. Displays the problematic URLs (HTTP and configured error messages expected on the page).
. Displays the tool progress %
. URLs are configurable in XML