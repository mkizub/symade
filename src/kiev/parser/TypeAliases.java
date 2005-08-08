package kiev.parser;

syntax TypeAliases {

typedef kiev.vlang.FileUnit         ASTFileUnit;
typedef kiev.vlang.Import           ASTImport;
typedef kiev.vlang.Typedef          ASTTypedef;
typedef kiev.parser.Opdef           ASTOpdef;
typedef kiev.vlang.Meta             ASTMeta;
typedef kiev.vlang.Initializer      ASTInitializer;
typedef kiev.vlang.WBCCondition     ASTInvariantDeclaration;
typedef kiev.vlang.WBCCondition     ASTRequareDeclaration;
typedef kiev.vlang.WBCCondition     ASTEnsureDeclaration;
typedef kiev.vlang.ConstExpr        ASTConstExpression;
}
