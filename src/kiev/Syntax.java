package kiev;

syntax Syntax {

typedef type@ kiev.stdlib.PVar<type>;
typedef type& kiev.stdlib.Ref<type>;
typedef type| kiev.stdlib.List<type>;

typedef kiev.stdlib.ListBuffer<kiev.vlang.ASTNode> ResPath;

}
