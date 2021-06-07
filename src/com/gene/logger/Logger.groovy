package com.gene.logger

/**
 * this methods receiving a Clousure in input are intended to be used as this:
 *
 * logger.info("this is the content of current directory", { sh 'ls'})
 *
 * this will print the message and then execute the code from the closure
 **/

 class Logger implements Serializable {
     private final Level Level
     private final Script scriptObj

     Logger(Script scriptObj, Level level) {
         this.scriptObj = scriptObj
         this.level = level
     }

     Level getLevel() {
         return level
     }

     void trace(String message, Exception e = null) {
         if (this.level > Level.TRACE) {
             return
         }

         scriptObj.ansiColor('xterm') {
             scriptObj.echo "\u001B[90m${message}\u001B[m"
             if (e !=null) {
                 e.printStackTrace()
             }
         }
     }

     void trace(Closure closure, String message) {
         if (this.level > Level.TRACE) {
             return
         }
         scriptObj.ansiColor('xterm') {
             scriptObj.echo "\u001B[90m${message}\u001B[m"
             if (closure != null) {
                 closure()
             }
         }
     }


     void debug(String message, Exception e = null) {
         if (this.level > Level.DEBUG) {
             return
         }

         scriptObj.ansiColor('xterm') {
             scriptObj.echo "\u001B[36m${message}\u001B[m"
             if (e !=null) {
                 e.printStackTrace()
             }
         }
     }

     void debug(Closure closure, String message) {
         if (this.level > Level.DEBUG) {
             return
         }
         scriptObj.ansiColor('xterm') {
             scriptObj.echo "\u001B[36m${message}\u001B[m"
             if (closure != null) {
                 closure()
             }
         }
     }
     

     void info(String message, Exception e = null) {
         if (this.level > Level.INFO) {
             return
         }

         scriptObj.echo "${message}"
         if (e !=null) {
             e.printStackTrace()
         }
     }

     void info(Closure closure, String message) {
         if (this.level > Level.INFO) {
             return
         }
         scriptObj.echo "${message}"
         if (closure != null) {
             closure()
         }
     }


     void warning(String message, Exception e = null) {
         if (this.level > Level.WARNING) {
             return
         }

         scriptObj.ansiColor('xterm') {
             scriptObj.echo "\u001B[33m${message}\u001B[m"
             if (e !=null) {
                 e.printStackTrace()
             }
         }
     }

     void warning(Closure closure, String message) {
         if (this.level > Level.WARNING) {
             return
         }
         scriptObj.ansiColor('xterm') {
             scriptObj.echo "\u001B[33m${message}\u001B[m"
             if (closure != null) {
                 closure()
             }
         }
     }


     void error(String message, Exception e = null) {
         if (this.level > Level.ERROR) {
             return
         }

         scriptObj.ansiColor('xterm') {
             scriptObj.echo "\u001B[31m${message}\u001B[m"
             if (e !=null) {
                 e.printStackTrace()
             }
         }
     }

     void error(Closure closure, String message) {
         if (this.level > Level.ERROR) {
             return
         }
         scriptObj.ansiColor('xterm') {
             scriptObj.echo "\u001B[31m${message}\u001B[m"
             if (closure != null) {
                 closure()
             }
         }
     }


     void fatal(String message, Exception e = null) {
         if (this.level > Level.FATAL) {
             return
         }

         scriptObj.ansiColor('xterm') {
             scriptObj.echo "\u001B[95m${message}\u001B[m"
             if (e !=null) {
                 e.printStackTrace()
             }
         }
     }

     void fatal(Closure closure, String message) {
         if (this.level > Level.FATAL) {
             return
         }
         scriptObj.ansiColor('xterm') {
             scriptObj.echo "\u001B[95m${message}\u001B[m"
             if (closure != null) {
                 closure()
             }
         }
     }

     void log(Level level, String message) {
         switch (level) {
             case Level.TRACE:
                trace(message)
                break
            case Level.DEBUG:
                debug(message)
                break
            case Level.INFO:
                info(message)
                break
            case Level.WARNING:
                warning(message)
                break
            case Level.ERROR:
                error(message)
                break
            case Level.FATAL:
                fatal(message)
                break
            case Level.OFF:
                break
         }
     }
 }