##Glossary

### Array-Wildcards

Array-Wildcards resemble [Catmandus concept of wildcards](http://librecat.org/Catmandu/#wildcards).

When working with arrays and repeated fields you can use wildcards to select all or certain elements of an array as well as select an additional new element.
You use them instead of the index number. These can also be used (some only) when generating new elements of an array.
- `*`: selects all elements of an array
- `$first`: selects only the first element of an array
- `$last`: selects only the last element of an array
- `$prepend`: selects the position infront of the first element array. This can be used, when generating new elements in an array.
- `$append`: selects the position behind the last element of an array. This can be used, when generating new elements in an array.

### General path wildcards

General path wildcards resemble [Metamorphs concept of wildcards](https://github.com/metafacture/metafacture-core/wiki/Metamorph-User-Guide#addressing-pieces-of-data)

Beside the array-wildcards you can use other general wildcards to select variations of an path. These wildcards cannot be used to generating new elements.
These wildcards are no part of the Catmandu Fix. They cannot be used (yet?) to select variation of whole paths but element names.

- `*` is a placeholder for for unlimited characters
- `?` is a placeholder for a single arbitrary character
- `|` allows for multiple versions either of the whole path or of parts, when used in a group `(...|...)`
- `[...]` can be used as placeholder for distinct characters
- 
