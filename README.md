Chunk Templates for Java
========================

[<img align="right" src= "http://www.x5dev.com/chunk/docs/img/chunk_logo_500x500.png" width="250" />][5]
Full documentation at http://www.x5dev.com/chunk/

Chunk is a Java Template Engine for applications that serve HTML or XML.  It's the little engine that can handle any templating job, plain text, markup, markdown, whatever, wherever.

Chunk is compact and speedy, easy to learn yet very powerful.

Chunk templates provide a rich featureset: in-tag filters, in-tag default values, includes, looping and branching, defining multiple snippets per template file, layered themes, macros, and much much more.

Free to use, copyright waived.  MIT License.

Quick Start
===========

Browse the [fabulous documentation][5] chock full of examples and recipes.

[Download the latest chunk-template jar][6]. Works with Java 1.5 and up.

Available from Maven Central:

```
    <dependency>
      <groupId>com.x5dev</groupId>
      <artifactId>chunk-templates</artifactId>
      <version>3.6.2</version>
    </dependency>
```

Features
========
  * Compatible with Android, GAE (Google App Engine), [Spring MVC][7].
  * Nestable looping and branching (if/elseIf/else).
  * Macros, includes and conditional includes.
  * Speedy rendering engine that pre-parses templates.
  * Curly-brace {$tags} pop nicely in a backdrop full of ```<AngleBrackets>``` (and your xml/html can still validate).
  * Flexible null-handling; template designer may easily specify default tag values.
  * Swiss army knife of chainable [filters][1] a la {$tag|trim}, including regex (regular expressions), sprintf.
  * Localization framework.
  * Rapid MVC: Glue a "model" object (or objects) to a "view" template with a single line of controller code.
  * Define multiple composable snippets per template file.
  * Stateless tags - encourages cleaner code via separation of concerns.
  * Support for theme layers with layered inheritance.
  * Hooks for extending - add your own filters, template loader, or tag protocol.
  * Eclipse Template Editor plugin available with syntax highlighting & more.

An [Eclipse plugin][3] provides syntax highlighting, outline navigation pane for snippets in .chtml files, and auto-hyperlinks of snippet references.  Requires Eclipse Helios (3.6) or better.  [Get the plugin][3].

----
Dependencies
============

Most features work fully with the standalone jar.  A couple advanced features can be unlocked by adding these libraries to the classpath:

 * Provide macro (exec) arguments in JSON format - download [jackson-databind][8], [jackson-core][9] and [jackson-annotations][10] or fetch via maven:
```
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>2.12.2</version>
    </dependency>
```

 * Enable jeplite-powered math expression evaluation via the |calc(...) filter - [download jeplite jar][11] or fetch via maven:
```
    <dependency>
      <groupId>org.cheffo</groupId>
      <artifactId>jeplite</artifactId>
      <version>0.8.7a</version>
    </dependency>
```

----

Android: Binding Beans to Template
==================================

Note: on Android (optional) - to make use of chunk.setToBean("tag",bean) binding, just make sure to include this additional dependency in your project:
```
    <dependency>
      <groupId>com.madrobot</groupId>
      <artifactId>madrobotbeans</artifactId>
      <version>0.1</version>
    </dependency>
```

Or [download madrobotbeans-0.1.jar][2] directly.  Thanks to Elton Kent and the [Mad Robot][4] project.

![Analytics](https://ga-beacon.appspot.com/UA-18933152-2/tomj74/chunk-templates)

  [1]: https://www.x5dev.com/chunk/wiki/Chunk_Tag_Filters
  [2]: https://github.com/tomj74/chunk-templates/releases/download/release-2.4/madrobotbeans-0.1.jar
  [3]: http://www.x5dev.com/chunk/wiki/index.php/Eclipse_Template_Editor_plugin
  [4]: https://code.google.com/p/mad-robot/
  [5]: https://www.x5dev.com/chunk/
  [6]: https://github.com/tomj74/chunk-templates/releases/latest
  [7]: https://www.x5dev.com/chunk/wiki/Spring_MVC
  [8]: https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.12.2/jackson-databind-2.12.2.jar
  [9]: https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.12.2/jackson-core-2.12.2.jar
  [10]: https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.12.2/jackson-annotations-2.12.2.jar
  [11]: https://github.com/tomj74/chunk-templates/releases/download/release-2.4/jeplite-0.8.7a.jar
