/**
 * 创建于 2019-04-08 22:19:52
 */
package cn.zhg.preprocessor.processor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacFiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;

/**
 * @author zzz
 */
public class FastProcessor extends AbstractProcessor {
    private JavacProcessingEnvironment javacProcessingEnv;
    private JavacFiler javacFiler;
    private Trees trees;
    private TreeMaker treeMaker;
    private boolean isDebug;
    private File debugFile;
    private boolean keep;
    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        Map<String, String> options = env.getOptions();
        System.out.println("初始化解析器,options=" + options);
        if (options != null) {
            if (options.containsKey("isDebug")) {
                isDebug = Boolean.parseBoolean(options.get("isDebug"));
            }
            if (options.containsKey("debugFile")) {
                debugFile = new File(options.get("debugFile"));
                if (!debugFile.exists()) {
                    debugFile.getParentFile().mkdirs();
                }
            }
            if (options.containsKey("keep")) {
                keep = Boolean.parseBoolean(options.get("keep"));
            }
        }
        if (env instanceof JavacProcessingEnvironment) {
            javacProcessingEnv = (JavacProcessingEnvironment) env;
        } else {
            log("非JavacProcessingEnvironment,env class=" + env.getClass());
            return;
        }
        trees = Trees.instance(javacProcessingEnv);
        treeMaker = TreeMaker.instance(javacProcessingEnv.getContext());
    }

    /**
     *
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (javacProcessingEnv == null || roundEnv.processingOver()) {
            return false;
        }
        for (Element element : roundEnv.getRootElements()) {
            log("class=" + element.getClass() + "," + "element=" + element);
            TreePath treePath = trees.getPath(element);
//				Logger.d("class="+treePath.getClass()+","+"treePath="+treePath);
            CompilationUnitTree unit = treePath.getCompilationUnit();
//				Logger.d("unit="+unit.getClass()+","+"unit="+unit);
            Tree tree = trees.getTree(element);
//            Logger.d("class=" + tree.getClass() + "," + "tree =" + tree);
            if (tree instanceof JCClassDecl) {
                JCClassDecl classDecl = (JCClassDecl) tree;
                ProcessTree<JCClassDecl> child = new ProcessTree(element, unit, classDecl);

                parseJCClassDecl(child);
            }
        }
        return true;
    }

    /**
     * 解析类
     *
     * @param parent
     */
    private void parseJCClassDecl(ProcessTree<JCClassDecl> parent) {
        JCClassDecl tree = parent.getTree();
        parent.setClassName(tree.name.toString());
//        Logger.d("class="+tree.name.toString());
        tree.implementing= deletePreprocessorInters(tree.implementing);
        if (tree.defs == null) {
            return;
        }
        tree.defs.forEach(item -> {
            if (item instanceof JCMethodDecl) {
                JCMethodDecl jcMethodDecl = (JCMethodDecl) item;
                ProcessTree<JCMethodDecl> child = new ProcessTree(parent, jcMethodDecl);

                parseJCMethodDecl(child);
                return;
            }
            if (item instanceof JCVariableDecl) {
                parseJCVariableDecl(new ProcessTree(parent, (JCVariableDecl) item));
                return;
            }
            if (item instanceof JCClassDecl) {
                JCClassDecl jcClassDecl = (JCClassDecl) item;
                ProcessTree<JCClassDecl> child = new ProcessTree(parent, jcClassDecl);

                parseJCClassDecl(child);
                return;
            }
            log("class=" + item.getClass() + ",item=" + item);
        });
    }

    /**
     * 删除cn.zhg.preprocessor.inter.CompilerConstant 接口
     *
     * @param implementing
     * @return
     */
    private com.sun.tools.javac.util.List<JCExpression> deletePreprocessorInters(com.sun.tools.javac.util.List<JCExpression> implementing) {
        if(keep){
            return implementing;
        }
        List<JCExpression> newImls = new ArrayList<>();
        boolean hashChange = false;
        for (JCExpression item : implementing) {
            if ("CompilerConstant".equals(item.toString())) {
                hashChange = true;
            } else {
                newImls.add(item);
            }
        }
        if (hashChange) {
            return com.sun.tools.javac.util.List.from(newImls);
        }
        return implementing;
    }

    /**
     * 解析方法
     *
     * @param parent
     */
    private void parseJCMethodDecl(ProcessTree<JCMethodDecl> parent) {
        JCMethodDecl tree = parent.getTree();
        parent.setMethodName(tree.name.toString());
        tree.body = parseJCBlock(new ProcessTree<>(parent, tree.body));
    }

    /**
     * 解析方法内容
     *
     * @param parent
     * @return
     */
    private JCBlock parseJCBlock(ProcessTree<JCBlock> parent) {
        JCBlock tree = parent.getTree();
        if (tree == null) {
            return tree;
        }
        tree.getStatements().forEach(item -> {
            if (item instanceof JCExpressionStatement) {
                parseJCExpressionStatement(new ProcessTree<JCExpressionStatement>(parent, (JCExpressionStatement) item));
                return;
            }
            log("class=" + item.getClass() + ",item=" + item);
        });
        return tree;
    }

    /**
     * 解析字段
     *
     * @param parent
     */
    private void parseJCVariableDecl(ProcessTree<JCVariableDecl> parent) {
        JCVariableDecl tree = parent.getTree();
        tree.init = parseJCExpression(new ProcessTree<>(parent, tree.init));
    }

    /**
     * 解析方法内容
     *
     * @param parent
     */
    private void parseJCExpressionStatement(ProcessTree<JCExpressionStatement> parent) {
        JCExpressionStatement tree = parent.getTree();
        JCExpression item = tree.expr;
        if (item == null) {
            return;
        }
        if (item instanceof JCMethodInvocation) {
            parseJCMethodInvocation(new ProcessTree<JCMethodInvocation>(parent, (JCMethodInvocation) item));
        } else {
            log("class=" + item.getClass() + ",item=" + item);
        }
    }

    /**
     * 解析方法调用
     *
     * @param parent
     */
    private JCMethodInvocation parseJCMethodInvocation(ProcessTree<JCMethodInvocation> parent) {
        JCMethodInvocation tree = parent.getTree();
        if (tree.meth != null) {
            tree.meth = parseJCExpression(new ProcessTree<>(parent, tree.meth));
        }
        if (tree.args != null) {
            boolean hashChange = false;//debug
            int size = tree.args.size();
            JCExpression[] newArgs = new JCExpression[size];
            for (int i = 0; i < size; i++) {
                JCExpression item = tree.args.get(i);
                newArgs[i] = parseJCExpression(new ProcessTree<>(parent, item));
                if (newArgs[i] != item) {
                    hashChange = true;
                }
            }
            if (hashChange) {
                tree.args = com.sun.tools.javac.util.List.from(newArgs);
                log("hashChange:" + tree.toString());
            }
        }
        return tree;
    }

    /**
     * 解析表达式,和替换
     *
     * @param parent
     * @return
     */
    private JCExpression parseJCExpression(ProcessTree<JCExpression> parent) {
        JCExpression tree = parent.getTree();
        if (tree instanceof JCIdent) {
            return parseJCIdent(new ProcessTree<>(parent, (JCIdent) tree));
        } else if (tree instanceof JCBinary) {
            return parseJCBinary(new ProcessTree<>(parent, (JCBinary) tree));
        } else if (tree instanceof JCMethodInvocation) {
            return parseJCMethodInvocation(new ProcessTree<>(parent, (JCMethodInvocation) tree));
        } else if (tree instanceof JCFieldAccess) {
            JCFieldAccess jcFieldAccess = (JCFieldAccess) tree;
            jcFieldAccess.selected = parseJCExpression(new ProcessTree<>(parent, jcFieldAccess.selected));
            return jcFieldAccess;
        } else if (tree instanceof JCNewClass) {
            return parseJCNewClass(new ProcessTree<>(parent, (JCNewClass) tree));
        } else if (tree instanceof JCLambda) {
            return parseJCLambda(new ProcessTree<>(parent, (JCLambda) tree));
        } else if (tree instanceof JCLiteral) {
            //debug
            return tree;
        } else {
            log("class=" + tree.getClass() + ",item=" + tree);
            return tree;
        }
    }

    /**
     * 解析lamba表达式
     *
     * @param parent
     * @return
     */
    private JCExpression parseJCLambda(ProcessTree<JCLambda> parent) {
        JCLambda tree = parent.getTree();
        if (tree.body != null) {
            if (tree.body instanceof JCBlock) {
                tree.body = parseJCBlock(new ProcessTree<>(parent, (JCBlock) tree.body));
            }
        }
        return tree;
    }

    /**
     * 解析创建对象,new表达式
     *
     * @param parent
     * @return
     */
    private JCExpression parseJCNewClass(ProcessTree<JCNewClass> parent) {
        JCNewClass tree = parent.getTree();
        parent.setClassName(tree.clazz.toString());
        if (tree.args != null) {
            boolean hashChange = false;//debug
            int size = tree.args.size();
            JCExpression[] newArgs = new JCExpression[size];
            for (int i = 0; i < size; i++) {
                JCExpression item = tree.args.get(i);
                ProcessTree<JCExpression> child = new ProcessTree<>(parent, item);
                newArgs[i] = parseJCExpression(child);
                if (newArgs[i] != item) {
                    hashChange = true;
                }
            }
            if (hashChange) {
                tree.args = com.sun.tools.javac.util.List.from(newArgs);
            }
        }
        return tree;
    }

    /**
     * 连接表达式
     *
     * @param parent
     * @return
     */
    private JCExpression parseJCBinary(ProcessTree<JCBinary> parent) {
        JCBinary tree = parent.getTree();
        tree.lhs = parseJCExpression(new ProcessTree<JCExpression>(parent, tree.lhs));
        tree.rhs = parseJCExpression(new ProcessTree<JCExpression>(parent, tree.rhs));
        return tree;
    }

    /**
     * 解析变量和转换
     *
     * @param jcIdent
     */
    private JCExpression parseJCIdent(ProcessTree<JCIdent> parent) {
        JCIdent tree = parent.getTree();
        if (tree == null) {
            return tree;
        }
        String name = tree.name.toString();
        switch (name) {
            case "__LINE__": {
                long line = parent.getLineNumber();
//                Logger.d("line=" + line);
                return treeMaker.Literal(line);
            }
            case "__COLUMN__": {
                long column = parent.getColumnNumber();
//                Logger.d("column=" + column);
                return treeMaker.Literal(column);
            }
            case "__FILE_PATH__": {
                String filePath = parent.getFilePath();
//                Logger.d("filePath=" + filePath);
                return treeMaker.Literal(filePath);

            }
            case "__FILE_NAME__": {
                String fileName = parent.getFileName();
//                Logger.d("fileName=" + fileName);
                return treeMaker.Literal(fileName);
            }
            case "__CLASS_NAME__": {
                String className = parent.getClassName();
//                Logger.d("className=" + className);
                return treeMaker.Literal(className);

            }
            case "__METHOD_NAME__": {
                String methodName = parent.getMethodName();
//                Logger.d("methodName=" + methodName);
                return treeMaker.Literal(methodName);

            }
            case "__LOCATION__": {
                String className = parent.getClassName();
                String fileName = parent.getFileName();
                long line = parent.getLineNumber();
                String methodName = parent.getMethodName();
                String location = className + "." + methodName + "(" + fileName + ":" + line + ")";
//                Logger.d("location=" + location);
                return treeMaker.Literal(location);
            }
            default:
                return tree;
        }
    }


    private void log(String msg, Throwable ex) {
        if (isDebug) {
            ex.printStackTrace();
            processingEnv.getMessager().printMessage(Kind.NOTE, msg);
            if (debugFile != null) {
                try (PrintStream out = new PrintStream(new FileOutputStream(debugFile, true))) {
                    out.println(msg);
                    ex.printStackTrace(out);
                } catch (Exception e) {
                }
            }
        }

    }

    private void log(String msg) {
        if (isDebug) {
            processingEnv.getMessager().printMessage(Kind.NOTE, msg);
            if (debugFile != null) {
                try (PrintStream out = new PrintStream(new FileOutputStream(debugFile, true))) {
                    out.println(msg);
                } catch (Exception e) {
                }
            }
        }
    }

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set = new HashSet<>();
        set.add("*");
        return set;
    }

}
