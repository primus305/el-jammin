# el-jammin

Desktop application for creating music through the improvisation.

## Prerequisites

- Leiningen
- SuperCollider

## Clone

- Clone this repo to your local machine using `https://github.com/primus305/el-jammin.git`

## Setup

- First, in your SuperCollider environment set server options

```supercollider
s.options.maxLogins = 8;
s.options.memSize = 65536;
```

- Then select your code from above and press <kbd> Shift + Enter </kbd> to execute it. Now you can boot SuperCollider server.

- After that, there are two ways to run the app:

	Using Emacs, open the file `el-jammin/src/el_jammin/core.clj`, then use <kbd>M-x</kbd> `cider-jack-in`

	or

	Using Leiningen

	```shell
	$ lein repl
	```

## About project

- Short video `https://drive.google.com/open?id=1MuKQPo5lyqpEVrGG_k4UJvmOuPZ3sZ85`

## Icon reference

- Icons8 - https://icons8.com

## License

Copyright © 2019 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
