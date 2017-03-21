# @Deprecated
## This repo is not being maintained any more. Please use [Lightning Browser](https://github.com/anthonycr/Lightning-Browser) instead.

# Thunder Browser [![Build Status](https://travis-ci.org/anthonycr/Thunder-Browser.svg?branch=master)](https://travis-ci.org/anthonycr/Thunder-Browser)
#### Speed, Simplicity, Security
#### A Traditional Browsing Experience
![](ic_launcher_small.png)
#### Download
* [Download APK from here](https://github.com/anthonycr/Thunder-Browser/releases)

* Download from Google Play (Not available just yet)


#### Features
* Speed of Lightning Browser with a more traditional experience

* Bookmarks

* History

* Multiple search engines (Google, Bing, Yahoo, StartPage, DuckDuckGo, etc.)

* Incognito mode

* Flash support (prior to 4.4)

* Follows Google design guidelines

* Google search suggestions

* Orbot Proxy support 

#### Permissions

* ````INTERNET````: For accessing the web

* ````WRITE_EXTERNAL_STORAGE````: For downloading files from the browser

* ````READ_EXTERNAL_STORAGE````: For downloading files from the browser

* ````ACCESS_FINE_LOCATION````: For sites like Google Maps, it is disabled by default in settings and displays a pop-up asking if a site may use your location when it is enabled

* ````READ_HISTORY_BOOKMARKS````: To synchronize history and bookmarks between the stock browser and Thunder

* ````WRITE_HISTORY_BOOKMARKS````: To synchronize history and bookmarks between the stock browser and Thunder

* ````ACCESS_NETWORK_STATE````: Required for the WebView to work for some OEM versions of WebKit

#### The Code
* Please contribute code back if you can. The code isn't perfect.
* Please add translations/translation fixes as you see need

#### Setting Up the Project
Due to the inclusion of the netcipher library for Orbot proxy support, importing the project will show you some errors. To fix this, first run the following git command in your project folder (NOTE: You need the git command installed to use this):
```
git submodule update --init --recursive
```
Once you run that command, the IDE should automatically import netcipher and a couple submodules in as separate projects. Than you need to set the netcipher library project as a libary of the browser project however your IDE makes you do that. Once those steps are done, the project should be all set up and ready to go.

#### License
```
Copyright 2014 Anthony Restaino

Thunder Browser

   This Source Code Form is subject to the terms of the 
   Mozilla Public License, v. 2.0. If a copy of the MPL 
   was not distributed with this file, You can obtain one at 
   
   http://mozilla.org/MPL/2.0/
```
