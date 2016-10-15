# ErrorT
How to use Cats - XorT for error handling

# Problem
In our codebase, most functions are returning one of `A`, `Option[A]`, `Future[A]` and `Future[Option[A]]`.

With `Option`, when we get `None`, we cannot identify the error.

Therefore, we don't know whcih error code we need to return to Frontend.

# Intention
- use `Xor` to capture the error
- use `XorT` to work with `Future`
- avoiding comprehensive refactoring
