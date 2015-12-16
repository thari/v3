/*
Copyright (c) 2015, Robby, Kansas State University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

grammar Antlr4Logika;

/*
==============================================
 Symbol     Unicode    ASCII
----------------------------------------------
   ⊥         22A5      _|_
   ≤         2264      <=
   ≥         2265      >=
   ≠         2260      !=
   ¬         00AC      not        !        ~
   ∧         2227      and        &&
   ∨         2228      or         ||
   →         21D2      implies    ->
   ∀         2200      forall     all      A
   ∃         2203      exists     some     E
   ⊢         22A2      |-         ---+
==============================================

Note: ---+ means at least three minus (-) characters
*/

@header {
// @formatter:off
}

sequentFile: sequent NL* proof? NL* EOF ;

proofFile: proof EOF ;

programFile: program EOF ;

sequent // note: all newlines inside a sequent are whitespaces
  : ( premises+=formula ( ',' premises+=formula )* )?
    tb=( '|-' | '⊢' )
    conclusions+=formula ( ',' conclusions+=formula )*
  | premises+=formula*
    tb=HLINE
    conclusions+=formula+
  ;

proof: tb='{' proofStep? ( NL+ proofStep? )* te='}' NL* ;

proofStep
  : tb=NUM '.' formula justification                    #Step
  | sub=NUM '.' NL* '{' NL*
    assume=NUM '.'
    ( fresh=ID
    | fresh=ID? formula ate='assume' )
    ( NL+ proofStep )*
    te='}'                                              #SubProof
  ;

formula
  : t=( '_|_' | '⊥' )                                   #Bottom  // propositional logic
  | tb=ID ( '.' te=ID /* $te.text=="size" */ )?         #Var     // propositional logic
  | tb='(' formula te=')'                               #Paren   // propositional logic
  | t='$result'                                         #Result  // program logic
  | fun=ID '(' formula ( ',' formula )* te=')'          #Apply   // predicate logic
  | t=NUM                                               #Int     // algebra
  | l=formula op=( '*' | '/' | '%' ) NL? r=formula      #Binary  // algebra
  | l=formula op=( '+' | '-' ) NL? r=formula            #Binary  // algebra
  | l=formula
    op=( '<' | '<=' | '≤' | '>' | '>=' | '≥' ) NL?
    r=formula                                           #Binary  // algebra
  | l=formula
    op=( '=' |'==' | '!=' | '≠' ) NL?
    r=formula                                           #Binary  // algebra
  | op='-' formula                                      #Unary   // algebra
  | op=( 'not' | '!' | '~' | '¬' ) formula              #Unary   // propositional logic
  | l=formula ( 'and' | '&&' | '∧' ) NL? r=formula      #Binary  // propositional logic
  | l=formula ( 'or' | '||' | '∨' ) NL? r=formula       #Binary  // propositional logic
  | l=formula ( 'implies' | '->' | '→' ) NL? r=formula  #Binary  // propositional logic
  | qformula                                            #Quant   // predicate logic
  ;

qformula
  : q=( 'forall' | 'all' | 'A' | '∀'
      | 'exists' | 'some' | 'E' | '∃' )
    gVars+=ID+ '|' NL? formula
  ;

justification
  : t='premise'                                         #Premise
  | ( tb='andi' | tb=( '&&' | '∧' ) ID ) // ID=="i"
    lStep=NUM rStep=NUM                                 #AndIntro
  | ( tb=('ande1' | 'ande2' )
    | tb=( '&&' | '∧' ) ID ) // ID=="e1" or ID=="e2"
    andStep=NUM                                         #AndElim
  | ( tb=( 'ori1' | 'ori2' )
    | tb=( '||' | '∨' ) ID ) // ID=="i1" or ID=="i2"
    orStep=NUM                                          #OrIntro
  | ( tb='ore' | tb=( '||' | '∨' ) ID ) // ID=="e"
    orStep=NUM lSubProof=NUM rSubProof=NUM              #OrElim
  | ( tb='impliesi' | tb=( '->' | '→' ) ID ) // ID=="i"
    impliesStep=NUM                                     #ImpliesIntro
  | ( tb='impliese' | tb=( '->' | '→' ) ID ) // ID=="e"
    impliesStep=NUM antecedentStep=NUM                  #ImpliesElim
  | ( tb=( 'noti' | 'negi' )
    | tb=( '!' | '~' | '¬' ) ID // ID=="i"
    ) subProof=NUM                                      #NegIntro
  | ( tb='note' | tb='nege'
    | tb=( '!' | '~' | '¬' ) ID // ID=="e"
    ) step=NUM notStep=NUM                              #NegElim
  | ( tb='bottome'
    | tb=('_|_' | '⊥' ) ID // ID=="e"
    ) falseStep=NUM                                     #BottomElim
  | tb='Pbc' subProof=NUM                               #Pbc
  | ( tb='foralli' | tb='alli' | tb='Ai'
    | tb='∀' ID // ID=="i"
    ) subProof=NUM                                      #ForallIntro
  | ( tb='foralle' | tb='alle' | tb='Ae'
    | tb='∀' ID // ID=="e"
    ) stepOrFact=numOrId formula+                       #ForallElim
  | tb=( 'existsi' | 'somei' | 'Ei' )
    existsStep=NUM formula+                             #ExistsIntro
  | tb=( 'existse' | 'somee' | 'Ee' )
    stepOrFact=numOrId subproof=NUM                     #ExistsElim
  | {"∃".equals(_input.LT(1).getText()) &&
     "i".equals(_input.LT(2).getText())}?
    tb='∃' ID existsStep=NUM formula+                   #ExistsIntro
  | tb='∃' t=ID // ID=="e"
    stepOrFact=numOrId subproof=NUM                     #ExistsElim
  | tb='algebra' steps+=NUM*                            #Algebra
  | tb='auto' stepOrFacts+=numOrId*                     #Auto
  ;

numOrId: t=( NUM | ID );

program
  : ( tb='import' org=ID '.' sireum=ID '.' 'logika' '.' te='_' NL+
      // org=="org" && sireum="sireum"
      ( lgk '"""' facts te='"""' )?
      stmts
    )?
  ;

stmts: stmt? ( NL+ stmt? )* ;

stmt
  : modifier=( 'var' | 'val' ) ID ':' type '=' exp      #VarDeclStmt
  | tb=ID '=' exp                                       #AssignVarStmt
  | tb='assert' '(' exp te=')'                          #AssertStmt
  | tb='if' '(' exp ')' NL* ttb='{' ts=stmts tte='}'
    'else' NL* ftb='{' fs=stmts fte='}'                 #IfStmt
  | tb='while' '(' exp ')' NL* ltb='{'
    ( NL* lgk '"""' loopInvariant '"""' )?
    stmts
    lte='}'                                             #WhileStmt
  | tb=ID '=' 'readInt' '(' STRING? te=')'              #ReadIntStmt
  | op=( 'print' | 'println' )
    '(' s=ID /* s=="size" */ STRING te=')'              #PrintStmt
  | ( id=ID '=' )? m=ID '(' ( exp ( ','exp )* )? te=')' #MethodInvocationStmt
  | tb=ID '=' ID '.' te=ID /* te=="clone" */            #SeqCloneStmt
  | tb=ID '(' exp ')' '=' exp                           #SeqAssignStmt
  | id=ID '=' exp op='+:' seq=ID                        #SeqPendStmt
  | id=ID '=' seq=ID op=':+' exp                        #SeqPendStmt
  | methodDecl                                          #MethodDeclStmt
  | lgk '"""'
    ( proof
    | sequent
    | invariants
    ) te='"""'                                          #LogikaStmt
  ;

lgk: t=( ID | 'logika' ) ; // t=="l" or t=="logika"

exp
  : t=NUM                                               #IntExp
  | tb=ID
    ( '(' exp te=')'
    | '.' te=ID // te=="size"
    )?                                                  #IdExp
  | tb=( 'BigInt' | 'Z' ) '(' STRING te=')'             #BigIntExp
  | tb=( 'Seq' | 'Zs' ) '(' exp ( ',' exp )? te=')'     #SeqExp
  | tb='(' exp te=')'                                   #ParenExp
  | op=( '-' | '!' ) exp                                #UnaryExp
  | l=exp op=( '*' | '/' | '%' ) NL? r=exp              #BinaryExp
  | l=exp op=( '+' | '-' )  NL? r=exp                   #BinaryExp
  | l=exp op=( '>' | '>=' | '<' | '<=' )  NL? r=exp     #BinaryExp
  | l=exp op=( '==' | '!=' )  NL? r=exp                 #BinaryExp
  | l=exp ( '&&' ) NL? r=exp                            #BinaryExp
  | l=exp ( '||' ) NL? r=exp                            #BinaryExp
  ;

type
  : t=( 'BigInt' | 'Z' )                                #IntType
  | tb='Seq' '[' NL* ( 'BigInt' | 'Z' ) NL* te=']'      #IntSeqType
  | t='Zs'                                              #IntSeqType
  ;

loopInvariant
  : tb='{' NL*
    itb='invariant'
    ( formula? ( NL+ formula? )* )
    modifies
    te='}' NL*
  ;

modifies
  : tb='modifies' ID+ NL* ;

methodDecl
  : tb='def' ID  NL?
    '(' ( param ( ',' param )* )? ')' ':' ( type | 'Unit' ) te='='
    mtb='{'
    ( NL* lgk '"""' methodContract NL* '"""' )?
    stmts
    rtb='return' exp? NL*
    mte='}'
  ;

funDecl
  : tb='def' ID  NL?
    '(' param ( ',' param )* ')' ':' type
  ;

param: tb=ID ':' type ;

methodContract
  : tb='{' NL*
    ( rtb='requires' NL* formula ( NL+ formula? )* )? NL*
    modifies?
    ( rte='ensures' NL* formula ( NL+ formula? )* )? NL*
    te='}'
  ;

invariants
  : tb='{' NL*
    itb='invariant' NL* formula? ( NL+ formula? )*
    te='}' NL*
  ;

facts
  : tb='{' NL*
    ftb='fact' NL*
    factOrFunDecl? ( NL+ factOrFunDecl? )*
    te='}' NL* ;

factOrFunDecl
  : fact
  | funDecl
  ;

fact: tb=ID '.' qformula ;

HLINE: '-' '-' '-'+ ;

NUM: '0' | '-'? [1-9] [0-9]* ;

ID: [a-zA-Z] [a-zA-Z0-9]* ;

STRING: '"' ('\u0020'| '\u0021'|'\u0023' .. '\u007F')* '"' ; // all printable chars and no escape chars

RESERVED
  : 'abstract' | 'case' | 'catch' | 'class'
  | 'do' | 'extends' | 'final'
  | 'finally' | 'for' | 'forSome' | 'implicit'
  | 'lazy' | 'macro' | 'match' | 'new'
  | 'null' | 'object' | 'override' | 'package' | 'private'
  | 'protected' | 'sealed' | 'super' | 'this'
  | 'throw' | 'trait' | 'try' | 'type'
  | 'with' | 'yield'
  | '<-' | '<:' | '<%' | '>:' | '#' | '@'
  ;

NL: '\r'? '\n' ;
// newlines are processed after lexing according to:
// http://www.scala-lang.org/files/archive/spec/2.11/01-lexical-syntax.html#newline-characters

LINE_COMMENT: '//' ~[\r\n]* -> skip ;

COMMENT: '/*' .*? '*/' -> skip ;

WS: [ \t\u000C]+ -> skip ;

ERROR_CHAR: . ;
