# DemoHvacApp

DemoHvacApp is Android Automotive app for showing temparature.
The application communicates with Vehicle HAL using Car service and displays the temperature changes.

<pre>
 ^^^^^^^^^^^^^        ^^^^^^^^^^^^^       ^^^^^^^
[ DemoHvacApp ] -->  [ Car Service ] --> [ VHAL ]
 ^^^^^^^^^^^^^        ^^^^^^^^^^^^^       ^^^^^^^
                                           |:|<br />
                                           |:| CAN Network | <--> CAN BUS<br />
                                           |:|<br />
 ^^^^^^^^^^^^^        ^^^^^^^^^^^^^       ^^^^^^^
[ DemoHvacApp ] <--  [ Car Service ] <-- [ VHAL ]
 ^^^^^^^^^^^^^        ^^^^^^^^^^^^^       ^^^^^^^   
</pre>

## Screenshots 📷
| <img src="Images/DemoHvacApp.PNG" width="550" height="410"> |
|:----------:|

## Demo 🎥
| <img src="Images/DemoHvacApp.gif" width="550" height="410"> |
|:----------:|

## Licence 📝
[![PyPI license](https://img.shields.io/pypi/l/ansicolortags.svg)](https://pypi.python.org/pypi/ansicolortags/)

Copyright (c) 2020 [Rohit Benake](https://github.com/rohitbenake)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

####
