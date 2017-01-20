package org.twak.siteplan.campskeleton;

/**
 *
 * @author twak
 */
public class WindowSkeleton extends PlanSkeleton
{
    Plan output;
    public WindowSkeleton(Plan input)
    {
        super();

        output = new Plan();

        this.plan = output;

        init();
    }

}
