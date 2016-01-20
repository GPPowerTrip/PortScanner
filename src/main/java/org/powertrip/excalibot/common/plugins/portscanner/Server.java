package org.powertrip.excalibot.common.plugins.portscanner;

import org.powertrip.excalibot.common.com.*;
import org.powertrip.excalibot.common.plugins.ArthurPlug;
import org.powertrip.excalibot.common.plugins.interfaces.arthur.KnightManagerInterface;
import org.powertrip.excalibot.common.plugins.interfaces.arthur.TaskManagerInterface;
import org.powertrip.excalibot.common.utils.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Jaime on 20/01/2016.
 * 03:37
 */
public class Server extends ArthurPlug{
	public Server(KnightManagerInterface knightManager, TaskManagerInterface taskManager) {
		super(knightManager, taskManager);
	}

	@Override
	public PluginHelp help() {
		return new PluginHelp().setHelp("Usage: portscanner address:<address> ports:<first-last> bots:<number of bots>");
	}


	@Override
	public TaskResult check(Task task) {
		TaskResult result = new TaskResult();

		Long total = taskManager.getKnightCount(task.getTaskId());
		Long recev = taskManager.getResultCount(task.getTaskId());

		result
				.setSuccessful(true)
				.setTaskId(task.getTaskId())
				.setResponse("total", total.toString())
				.setResponse("done", recev.toString())
				.setComplete(total.equals(recev));
		return result;
	}

	@Override
	public TaskResult get(Task task) {
		Long total = taskManager.getKnightCount(task.getTaskId());
		Long recev = taskManager.getResultCount(task.getTaskId());

		TaskResult result = new TaskResult()
				.setTaskId(task.getTaskId())
				.setSuccessful(true)
				.setComplete(total.equals(recev));

		List<String> portList = taskManager.getAllResults(task.getTaskId())
				.stream()
				.filter(str -> str.getResponseMap().containsKey("port"))
				.map(str -> str.getResponse("port"))
				.collect(Collectors.toList());

		return result.setResponse("stdout", "Open ports: " +
				String.join(", ",
								portList.stream()
										.map(Integer::parseInt)
										.sorted()
										.map(String::valueOf)
										.collect(Collectors.toList())
							)
		);
	}

	@Override
	public void handleSubTaskResult(Task task, SubTaskResult subTaskResult) {
		/**
		 * Only if I need to do anything when I get a reply.
		 */
	}

	@Override
	public TaskResult submit(Task task) {
		//Get my parameter map, could use task.getParameter(String key), but this is shorter.
		Logger.log(task.toString());
		Map args = task.getParametersMap();

		//Declare my parameters
		String address;
		Long[] ports = new Long[2];
		long botCount;

		//Create a TaskResult and fill the common fields.
		TaskResult result = new TaskResult()
				.setTaskId(task.getTaskId())
				.setSuccessful(false)
				.setComplete(true);

		//No Dice! Wrong parameters.
		if( !args.containsKey("address") || !args.containsKey("ports") || !args.containsKey("bots") ) {
			return result.setResponse("stdout", "Wrong parameters");
		}

		//Parse parameters
		address = (String) args.get("address");
		String[] tmp = ((String)args.get("ports")).split("-");
		ports[0] = Long.parseLong(tmp[0]);
		ports[1] = Long.parseLong(tmp[1]);
		botCount = Long.parseLong((String) args.get("bots"));

		long range = ports[1] - ports[0] + 1;

		try {
			//Get bots alive in the last 50 seconds and get as many as needed
			List<KnightInfo> bots = knightManager.getFreeKnightList(50000).subList(0, (int) botCount);
			int i = 0;
			long step = 0;
			long ini = ports[0];
			for(KnightInfo bot : bots){
				ini += step;

				step = load(range, botCount, i++);
				knightManager.dispatchToKnight(
						new SubTask(task, bot)
								.setParameter("address", address)
								.setParameter("start", String.valueOf(ini))
								.setParameter("step", String.valueOf(step))
				);
			}
			result
					.setSuccessful(true)
					.setResponse("stdout", "Task accepted, keep an eye out for the results :D");
		}catch (IndexOutOfBoundsException e) {
			//No bots...
			result.setResponse("stdout", "Not enough free bots.");
		}
		return result;
	}

	public static long load(long ports, long bots, long index){
		//Dark Magic of darkness, blame Morgana
		return !(index-(ports % bots)<0)?ports/bots: (long) Math.ceil(ports/(double)bots);
	}
}
