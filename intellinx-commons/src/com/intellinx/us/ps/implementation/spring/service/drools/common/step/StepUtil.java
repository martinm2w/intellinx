package com.intellinx.us.ps.implementation.spring.service.drools.common.step;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.drools.command.Command;
import org.drools.command.CommandFactory;
import org.drools.runtime.StatelessKnowledgeSession;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.util.Assert;

import com.intellinx.us.ps.implementation.spring.service.drools.stateless.KnowledgeSessionService;

/**
 * 
 * @author Renato Mendes
 * 
 */
public class StepUtil {

	private SpelExpressionParser parser = new SpelExpressionParser();

	/**
	 * 
	 * @param step
	 */
	public void prepare(HqlStep step) {

		if (step.getQuery() == null) {
			Assert.notNull(step.getQuery());
		}

		if (step.getQuery().getParameters() != null
				&& !step.getQuery().getParameters().isEmpty()) {

			step.getQuery().setParametersExpressions(
					new HashMap<String, Expression>());

			for (String key : step.getQuery().getParameters().keySet()) {

				String expressionString = step.getQuery().getParameters()
						.get(key);

				step.getQuery().getParametersExpressions()
						.put(key, parser.parseExpression(expressionString));

			}

		}

	}

	/**
	 * 
	 * @param step
	 */
	public void prepare(AbstractStep step) {
		if (step.getWhen() != null) {
			step.setWhenExpressionParsed(parser.parseExpression(step.getWhen()));
		}
	}

	/**
	 * 
	 * @param step
	 */
	public void prepare(ExpressionStep step) {
		step.setExpressionsParsed(parser.parseExpression(step.getExpression()));
	}

	/**
	 * 
	 * @param service
	 * @param commands
	 * @param message
	 * @param knowledgeSession
	 * @param standardEvaluationContext
	 * @param step
	 * @param entityManagerFactory
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public void createBatchExecutionCommand(KnowledgeSessionService service,
			List<Command<?>> commands, Message<?> message,
			StatelessKnowledgeSession knowledgeSession,
			StandardEvaluationContext standardEvaluationContext, HqlStep step,
			EntityManagerFactory entityManagerFactory)
			throws SecurityException, IllegalArgumentException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {

		EntityManager entityManager = EntityManagerFactoryUtils
				.getTransactionalEntityManager(entityManagerFactory);

		List<?> objects = service.executeQuery(step.getQuery(), message,
				entityManager);

		addCommands(objects, commands, step);

	}

	/**
	 * 
	 * @param service
	 * @param commands
	 * @param message
	 * @param knowledgeSession
	 * @param context
	 * @param step
	 * @param entityManagerFactory
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public void createBatchExecutionCommand(KnowledgeSessionService service,
			List<Command<?>> commands, Message<?> message,
			StatelessKnowledgeSession knowledgeSession,
			StandardEvaluationContext context, ExpressionStep step,
			EntityManagerFactory entityManagerFactory)
			throws SecurityException, IllegalArgumentException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {

		Object value = step.getExpressionsParsed().getValue(context, message);

		addCommands(value, commands, step);

	}

	/**
	 * 
	 * @param object
	 * @param commands
	 * @param step
	 */
	private void addCommands(Object object, List<Command<?>> commands,
			AbstractStep step) {

		if (object != null) {

			switch (step.getTarget()) {
			case FACT:
				if (object instanceof Collection<?>) {
					commands.add(CommandFactory
							.newInsertElements((Collection<?>) object));
				} else {
					commands.add(CommandFactory.newInsert(object));
				}
				break;
			case GLOBALS:
				commands.add(CommandFactory.newSetGlobal(step.getParameter(),
						object));
				break;
			default:
				break;
			}

		}

	}
}
