import com.amowpuzzle.aitricker.domain.generator.*;
import com.amowpuzzle.aitricker.domain.model.*;
import com.amowpuzzle.aitricker.domain.engine.*;
public class ProbeCampaign {
 public static void main(String[]args){
  int[][] specs={{6,7,8,10,1,2,3},{10,12,28,30,2,2,5},{13,15,42,40,2,2,6},{16,18,65,60,2,2,6},{18,22,85,70,1,2,7},{18,24,90,80,1,2,8}};
  for(int[]v:specs){
   int minN=999,maxN=0,minD=999,maxD=0,maxF=0;long start=System.currentTimeMillis();
   for(int id=1;id<=10;id++){
    LevelSpec s=new LevelSpec(id,v[0],v[1],Difficulty.HARD,v[2],v[3],v[4],3,12,0.60f,v[5],v[6],0.45f,30);
    Level l=LevelGenerator.INSTANCE.generate(s); SolutionResult r=PuzzleSolver.INSTANCE.solve(l,l.getArrows());
    minN=Math.min(minN,l.getArrowCount());maxN=Math.max(maxN,l.getArrowCount());minD=Math.min(minD,r.getMaxDependencyDepth());maxD=Math.max(maxD,r.getMaxDependencyDepth());maxF=Math.max(maxF,r.getInitiallyFree());
   }
   System.out.printf("%dx%d count %d..%d depth %d..%d maxfree%d time%dms%n",v[0],v[1],minN,maxN,minD,maxD,maxF,System.currentTimeMillis()-start);
  }
 }
}
