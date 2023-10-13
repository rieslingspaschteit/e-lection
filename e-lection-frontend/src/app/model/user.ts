import { ConfigError, Messages } from './error'

/**
 * Constants describing roles a user can have.
 * A user can have multiple roles at the same time.
 * Each role allows the user to access certain functionality.
 */
export enum UserRole {

  /**
     * If a user is registered voter for at least one election, the user is considered a voter.
     */
  VOTER = 'voter',

  /**
     * If a user is registered trustee for at least one election, the user is considered a trustee.
     */
  TRUSTEE = 'trustee',

  /**
     * If the host set the users email as authority, the user is considered a authority
     */
  AUTHORITY = 'authority',
}

/**
 * This class describes a User with fields that hold data provided by the backend
 */
export class User {
  private static readonly ROLES = [UserRole.VOTER, UserRole.TRUSTEE, UserRole.AUTHORITY]

  private constructor (
    private readonly _roles: UserRole[],
    private readonly _email: string
  ) {}

  public get roles (): UserRole[] { return this._roles }
  public get email (): string { return this._email }

  public static fromJSON (json: any): User {
    if (json.userRoles === undefined || json.email === undefined) {
      throw new ConfigError(Messages.MISSING_ARGUMENT)
    }
    if (!Array.isArray(json.userRoles)) {
      throw new ConfigError(Messages.OTHER)
    }
    if (typeof json.email !== 'string') {
      throw new ConfigError(Messages.OTHER)
    }
    const roles = (json.userRoles as string[])
      .map(role => {
        const userRole = User.ROLES.find(userRole => userRole === role.toLocaleLowerCase())
        if (userRole === undefined) {
          throw new ConfigError(Messages.OTHER)
        }
        return userRole
      })

    return new User(roles, json.email)
  }
}
