grammar sLang;

WS: [ \t\r\n]+ -> skip;

// Parser rules
start: CITY name '{' blocks '}';
CITY: 'city';
name: '"' STRING '"';
blocks: block (blocks | /* epsilon */ ) ;
block: block_name name '{' commands '}';
block_name: 'road' | 'building' | 'stadium' | 'arena';
commands: command (commands | /* epsilon */ );
command: 'line(' point ',' point ')';
point: '(' DOUBLE ',' DOUBLE ')';

// Lexer rules
STRING: [a-zA-Z ]+;
DOUBLE: [0-9]+('.' [0-9]+)?;

// Define additional rules if necessary