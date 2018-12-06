# Nero (aka the Neovim REPL)

Nero is a very simple Neovim REPL.

It connects to neovim using RPC infrastructure and allows you to read-eval-print-loop over the
current lua state.

# How to get that working:

- `:call serverstart("127.0.0.1:12345")` on neovim;
  - (until propert argument handling is implemented)

- `sbt run` or `sbt assembly && java -jar target/scala-2.12/nero.jar`

# What can you do

It starts as a lua REPL.
That is exactly the same as a lua REPL, except that running from neovim's lua state.

If you want to, you can tinker around with VimL.
Just type `:set viml` on nero and it'll change the mode to viml:
```
[Lua ] > :set viml
[VimL] :
```

To get back to lua, just type `:set lua`:
```
[VimL] : :set lua
[Lua ] >
```

Also, `:q` quits.

Other commands are intended to be implemented in the future.


# TODO

- [ ] Build scripts to generate a single executable and/or shortcut;
- [ ] Better lua support
  - [x] Return values for expressions
- [ ] Better viml support
- [ ] Better REPL handling:
  - [ ] Autocompletion;
  - [ ] History (arrows);
  - [ ] Clear REPL;
  - [ ] Docs;
- [ ] Compile with graal-vm.
