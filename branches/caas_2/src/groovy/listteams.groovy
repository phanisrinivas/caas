import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.main.Environment;

def sys = Caas.getDefaultSystem()

for (org in sys.organizations)
{
    println org.name
    for (team in org.teams)
    {
        println '\t' + team.name
        for (a in team.assignments)
        {
            println '\t\t' + a.name
        }
    }  
}
