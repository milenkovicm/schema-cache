addSbtPlugin("com.dwijnand"      % "sbt-dynver"      % "3.1.0")
addSbtPlugin("com.dwijnand"      % "sbt-travisci"    % "1.1.1")
addSbtPlugin("com.geirsson"      % "sbt-scalafmt"    % "1.5.1")
addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "5.0.0")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"     % "0.3.4") //sbt dependencyUpdates

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25" // Needed by sbt-git
