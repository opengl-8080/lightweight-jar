package lwjar.precompile;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithPrivateModifier;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.PrettyPrinterConfiguration;
import com.sun.org.apache.regexp.internal.RE;
import lwjar.GlobalOption;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class JavaSourceCompressor {
    private final CompressLevel level;

    public JavaSourceCompressor(CompressLevel compressLevel) {
        this.level = Objects.requireNonNull(compressLevel);
    }

    public String compress(ProcessingFile file) {
        try {
            CompilationUnit cu = JavaParser.parse(file.path(), GlobalOption.getEncoding());
            return this.removeLineSeparatorAndComments(cu);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String compress(Path javaFilePath) {
        try {
            CompilationUnit cu = JavaParser.parse(javaFilePath, GlobalOption.getEncoding());
            return this.removeLineSeparatorAndComments(cu);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    private String removeLineSeparatorAndComments(CompilationUnit cu) {
        PrettyPrinterConfiguration conf = new PrettyPrinterConfiguration();

        if (this.level.satisfies(CompressLevel.REMOVE_LINE_SEPARATOR)) {
            conf.setIndent("");
            conf.setEndOfLineCharacter(" ");
        }

        if (this.level.satisfies(CompressLevel.REMOVE_COMMENTS)) {
            conf.setPrintComments(false);
            conf.setPrintJavaDoc(false);
        }

        if (this.level.satisfies(CompressLevel.REMOVE_ANNOTATIONS)) {
            List<AnnotationExpr> removeTargetAnnotations = new ArrayList<>();

            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MarkerAnnotationExpr n, Void arg) {
                    if (isRemoveTarget(n)) {
                        removeTargetAnnotations.add(n);
                    }
                    super.visit(n, arg);
                }
                @Override
                public void visit(SingleMemberAnnotationExpr n, Void arg) {
                    if (isRemoveTarget(n)) {
                        removeTargetAnnotations.add(n);
                    }
                    super.visit(n, arg);
                }
                @Override
                public void visit(NormalAnnotationExpr n, Void arg) {
                    if (isRemoveTarget(n)) {
                        removeTargetAnnotations.add(n);
                    }
                    super.visit(n, arg);
                }

                @Override
                public void visit(FieldDeclaration n, Void arg) {
                    this.removePrivateModifier(n);
                    super.visit(n, arg);
                }

                @Override
                public void visit(MethodDeclaration n, Void arg) {
                    this.removePrivateModifier(n);
                    super.visit(n, arg);
                }

                @Override
                public void visit(ConstructorDeclaration n, Void arg) {
                    this.removePrivateModifier(n);
                    super.visit(n, arg);
                }

                @Override
                public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                    if (n.isInnerClass() || n.isNestedType()) {
                        this.removePrivateModifier(n);
                    }
                    super.visit(n, arg);
                }

                @Override
                public void visit(EnumDeclaration n, Void arg) {
                    if (n.isNestedType()) {
                        this.removePrivateModifier(n);
                    }
                    super.visit(n, arg);
                }

                @Override
                public void visit(AnnotationDeclaration n, Void arg) {
                    if (n.isNestedType()) {
                        this.removePrivateModifier(n);
                    }
                    super.visit(n, arg);
                }

                private void removePrivateModifier(NodeWithPrivateModifier node) {
                    if (level.satisfies(CompressLevel.REMOVE_PRIVATE_MODIFIERS)) {
                        if (node.isPrivate()) {
                            node.removeModifier(Modifier.PRIVATE);
                        }
                    }
                }
            }, null);

            // remove annotations.
            removeTargetAnnotations.forEach(a -> a.getParentNode().ifPresent(p -> p.remove(a)));
        }

        return cu.toString(conf);
    }

    private static final Set<String> REMOVE_TARGET_ANNOTATION_NAMES;
    static {
        Set<String> set = new HashSet<>();
        set.add("Override");
        set.add("Deprecated");
        set.add("Documented");
        set.add("SuppressWarnings");
        set.add("FunctionalInterface");
        set.add("SafeVarargs");
        REMOVE_TARGET_ANNOTATION_NAMES = Collections.unmodifiableSet(set);
    }

    private boolean isRemoveTarget(AnnotationExpr annotation) {
        String name = annotation.getName().asString();
        return REMOVE_TARGET_ANNOTATION_NAMES.contains(name);
    }
    
    enum CompressLevel {
        REMOVE_COMMENTS(1),
        REMOVE_LINE_SEPARATOR(2),
        REMOVE_ANNOTATIONS(3),
        REMOVE_PRIVATE_MODIFIERS(4),
        RENAME_LOCAL_VARIABLES(5),
        ;
        
        private final int level;
        private static final Map<Integer, CompressLevel> mapping;
        static {
            Map<Integer, CompressLevel> map = new HashMap<>();
            map.put(1, REMOVE_COMMENTS);
            map.put(2, REMOVE_LINE_SEPARATOR);
            map.put(3, REMOVE_ANNOTATIONS);
            map.put(4, REMOVE_PRIVATE_MODIFIERS);
            map.put(5, RENAME_LOCAL_VARIABLES);
            mapping = Collections.unmodifiableMap(map);
        }
        
        static CompressLevel valueOf(Integer level) {
            return mapping.getOrDefault(level, RENAME_LOCAL_VARIABLES);
        }

        CompressLevel(int level) {
            this.level = level;
        }
        
        boolean satisfies(CompressLevel threshold) {
            return threshold.level <= this.level;
        }
    }
}
