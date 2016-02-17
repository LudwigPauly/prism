package parser.visitor;

import java.util.List;
import java.util.Vector;

import parser.Values;
import parser.ast.Declaration;
import parser.ast.DeclarationInt;
import parser.ast.DeclarationIntView;
import parser.ast.Expression;
import parser.ast.ExpressionVar;
import parser.ast.Module;
import parser.ast.ModulesFile;
import parser.type.TypeInt;
import prism.PrismLangException;
import prism.PrismUtils;

public class ExplodeBits extends ASTTraverseModify
{
	private Values constantValues;

	public ExplodeBits(Values constantValues)
	{
		this.constantValues = constantValues;
	}

	public void visitPost(ModulesFile mf) throws PrismLangException
	{
		List<Declaration> declarations = mf.getGlobals();
		List<Declaration> views = mf.getGlobalViews();

		if (!views.isEmpty()) {
			throw new PrismLangException("Can not explode bits for a model that already has views...");
		}

		explode(declarations, views);
	}

	public void visitPost(Module m) throws PrismLangException
	{
		List<Declaration> declarations = m.getDeclarations();
		List<Declaration> views = m.getViewDeclarations();

		if (!views.isEmpty()) {
			throw new PrismLangException("Can not explode bits for a model that already has views...");
		}

		explode(declarations, views);
	}

	private void explode(List<Declaration> declarations, List<Declaration> views) throws PrismLangException
	{
		for (int i = 0; i < declarations.size(); i++) {
			// iterate over declarations
			// this will also iterate over the newly added bits, but they are ignored (range = 2)
			Declaration decl = declarations.get(i);

			if (!(decl.getDeclType() instanceof DeclarationInt))
				continue;
			DeclarationInt declInt = (DeclarationInt) decl.getDeclType();
			checkForConstants(declInt.getLow());
			int low = declInt.getLow().evaluateInt(constantValues);
			checkForConstants(declInt.getHigh());
			int high = declInt.getHigh().evaluateInt(constantValues);
			int range = high - low + 1;
			if (range <= 2)  // don't bother
				continue;

			int bits = (int) Math.ceil(PrismUtils.log2(range));
			DeclarationIntView declView = new DeclarationIntView(Expression.Int(low), Expression.Int(high));

			declarations.remove(i);  // remove old variable
			int t = i;  // the target index
			for (int j = bits-1; j >= 0; j--) {
				String bitName = decl.getName()+"__bit_"+j;
				declarations.add(t, new Declaration(bitName, new DeclarationInt(Expression.Int(0), Expression.Int(1))));
				t++;  // increment to add next bit (lower) after the current bit
				declView.addBit(new ExpressionVar(bitName, TypeInt.getInstance()));
			}

			Declaration view = new Declaration(decl.getName(), declView);
			if (decl.isStartSpecified()) {
				view.setStart(decl.getStart().deepCopy());
			}
			views.add(view);
		}
	}
	
	private void checkForConstants(Expression e) throws PrismLangException
	{
		Vector<String> constants = e.getAllConstants();
		for (String constant : constants) {
			if (constantValues == null || !constantValues.contains(constant)) {
				throw new PrismLangException("Can not explode bits, constant '" + constant + "' has to be defined in the model file");
			}
		}
	}

}
