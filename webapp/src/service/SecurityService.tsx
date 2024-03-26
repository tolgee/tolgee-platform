export type AfterLoginLink = {
  url: string;
  // if userId is present after login link will only apply for that given user
  userId?: number;
};

export class SecurityService {
  public saveAfterLoginLink = (afterLoginLink: AfterLoginLink) => {
    localStorage.setItem('afterLoginLink', JSON.stringify(afterLoginLink));
  };

  public getAfterLoginLink = (): AfterLoginLink | null => {
    const link = localStorage.getItem('afterLoginLink');
    if (link) {
      return JSON.parse(link);
    }
    return null;
  };

  public removeAfterLoginLink = () => {
    return localStorage.removeItem('afterLoginLink');
  };
}

export const securityService = new SecurityService();
