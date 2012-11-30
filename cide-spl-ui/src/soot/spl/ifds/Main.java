package soot.spl.ifds;

import java.util.Map;

import net.sf.javabdd.BDDFactory;

import org.eclipse.jdt.core.IJavaProject;

import soot.Body;
import soot.Pack;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.options.Options;
import soot.spl.cflow.ConditionalControlDependenceGraph;
import soot.spl.cflow.LabeledUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import br.ufal.cideei.features.CIDEFeatureExtracterFactory;
import br.ufal.cideei.features.IFeatureExtracter;
import br.ufal.cideei.handlers.AnalysisArgs;
import br.ufal.cideei.soot.instrument.FeatureModelInstrumentorTransformer;

public class Main {
	
	public static long beforeSootStart;
	
	public static void main(final String classpath, String mainClass, final IJavaProject javaProject, final AnalysisArgs arg) {
		try {
			String j2me = arg.j2me ? "-j2me -x org. -x com. -x java -ire -allow-phantom-refs " : "";
			
			String string = "-cp "+classpath+" -f none -src-prec java -app -w "+j2me+"-main-class "+mainClass+" "+mainClass;
			String[] args = string.split(" ");
			
			if(!arg.includeJDK) Options.v().set_no_bodies_for_excluded(true);
			
			Pack pack = PackManager.v().getPack("wjtp");
			if(pack.get("wjtp.ifds")==null) {
				pack.add(new Transform("wjtp.ifds", new SceneTransformer() {

					protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
						IFeatureExtracter extracter = CIDEFeatureExtracterFactory.getInstance().getExtracter();
						FeatureModelInstrumentorTransformer bodyTransformer = new FeatureModelInstrumentorTransformer(extracter,classpath);
						for(SootClass sc: Scene.v().getApplicationClasses()) {
							for(SootMethod m: sc.getMethods()) {
								if(m.hasActiveBody()) {
									Body b = m.getActiveBody();
									bodyTransformer.transform(b);
								}
							}
						}
						
						Constraint.FACTORY = BDDFactory.init(100000, 100000);
						Constraint.FACTORY.setVarNum(100); //some number high enough to accommodate the max. number of features; ideally we should compute this number 

						Body mmBody = Scene.v().getMainMethod().getActiveBody();
						System.err.println(mmBody);
						
						ConditionalControlDependenceGraph<String, Unit> cpdg = new ConditionalControlDependenceGraph<String, Unit>(new LabeledUnitGraph(new ExceptionalUnitGraph(mmBody)));
						System.err.println();
					
//						final Multimap<SootMethod,Local> initialSeeds = HashMultimap.create();
//						initialSeeds.put(Scene.v().getMainMethod(), Scene.v().getMainMethod().getActiveBody().getLocals().getFirst());
//												
////						IFDSTabulationProblem<Unit,Value,SootMethod> problem = new heros.problems.IFDSUninitializedVariables();
////						
////						
////						SPLIFDSSolver<Value,SootMethod> solver = new SPLIFDSSolver<Value,SootMethod>(problem,alloyFilePath,numFeaturesPresent);	
////						long before = System.currentTimeMillis();
////						System.err.println("Starting solver...");
////						solver.solve();
////						System.err.println("Solving took: "+(System.currentTimeMillis()-before));
////						System.err.println(Scene.v().getMainMethod().getActiveBody());
////						Unit ret = Scene.v().getMainMethod().getActiveBody().getUnits().getLast();
////						for(Entry<Value, Constraint<String>> l: solver.resultsAt(ret).entrySet()) {
////							System.err.print(l.getKey());
////							System.err.print("=");
////							System.err.println(l.getValue().toString(bodyTransformer.getFeatureNumberer()));
////						}
////						System.err.println();
////						System.err.println(Constraint.FACTORY.getCacheStats());
//						
////						IFDSTabulationProblem<Unit, ?, SootMethod> problem = new heros.problems.IFDSReachingDefinitions();
//						IFDSTabulationProblem<Unit, ?, SootMethod, ?> problem;
//						try {
//							Class<IFDSTabulationProblem<Unit, ?, SootMethod, ?>> clazz = (Class<IFDSTabulationProblem<Unit, ?, SootMethod, ?>>) Class.forName("heros.problems."+arg.analysisClassName);
//							Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
//							problem = (IFDSTabulationProblem<Unit, ?, SootMethod, ?>) constructor.newInstance( new JimpleBasedInterproceduralCFG() );
//						} catch (Exception e) {
//							throw new RuntimeException(e);
//						} 
//						
//						PerformanceTestDriver.perfTest(bodyTransformer, problem, javaProject, arg);
					}

				}));
			}
			
			beforeSootStart = System.currentTimeMillis();
			soot.Main.main(args);
		} catch(RuntimeException e) {
			e.printStackTrace();
		}
	}

}
