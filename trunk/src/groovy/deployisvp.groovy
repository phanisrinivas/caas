import org.kisst.cordys.caas.Caas

if (args.length !=4)
{
 println "Usage : targetsystem isvpname isvppath promptsetsFilePath"
 return;
}

targetSystem = args[0]
isvpName = args[1]
isvpFilePath = args[2]
isvpPromptsetsFilePath = args[3]

println String.format('Deployin %1s isvp on target system %2s', isvpFilePath,targetSystem)

cordysEnv = Caas.getSystem(targetSystem)

//ISVP doesn't exist
if (cordysEnv.isvp[isvpName]==null)
{
	cordysEnv.loadIsvp(isvpFilePath,isvpPromptsetsFilePath)
}
else
{
	cordysEnv.upgradeIsvp(isvpFilePath,isvpPromptsetsFilePath)
}
