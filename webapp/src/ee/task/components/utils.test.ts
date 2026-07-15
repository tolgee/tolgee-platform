import { TaskModel, taskReportFileName } from './utils';

const task = (name: string, number: number): TaskModel =>
  ({ name, number } as TaskModel);

describe('taskReportFileName', () => {
  it('lowercases and underscores the task name', () => {
    expect(taskReportFileName(task('My Task', 3))).toBe('my_task_report.xlsx');
  });

  it('falls back to the task number when the name is empty', () => {
    expect(taskReportFileName(task('', 7))).toBe('task_7_report.xlsx');
  });
});
